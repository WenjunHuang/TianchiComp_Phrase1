package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents._
import io.netty.buffer.{ByteBuf, ByteBufUtil}
import io.netty.channel._
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ProviderAgentHandler(cap: CapacityType.Value, failRetry: (CapacityType.Value, BenchmarkRequest) => Unit)(implicit ec: ExecutionContext) extends ChannelDuplexHandler {
  val workingRequests: mutable.LongMap[BenchmarkRequest] = mutable.LongMap()

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    msg match {
      case req: BenchmarkRequest =>
        workingRequests(req.requestId) = req
        req.byteBuf.retain()
        ctx.writeAndFlush(req.byteBuf, ctx.voidPromise())
      case any =>
        ReferenceCountUtil.release(any)
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        for {
          isEvent <- DubboMessage.extractIsEvent(buf)
          requestId <- DubboMessage.extractRequestId(buf)
          status <- DubboMessage.extractStatus(buf)
        } {
          if (!isEvent) {
            workingRequests.remove(requestId) match {
              case Some(req) =>
                if (status == 20) {
                  ReferenceCountUtil.release(req.byteBuf)
                  val channel = req.replyTo
                  Future {
                    channel.writeAndFlush(BenchmarkResponse.toHttpResponse(buf), channel.voidPromise())
                  }
                } else {
                  ReferenceCountUtil.release(buf)

                  ctx.executor().execute { () =>
                    failRetry(cap, req)
                  }
                }
              case None =>
                ReferenceCountUtil.release(buf)
            }
          } else {
            ReferenceCountUtil.release(buf)
          }
        }
      case any =>
        ReferenceCountUtil.release(any)
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }
}
