package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, CapacityType, DubboMessage}
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable

class ProviderAgentHandler(val cap: CapacityType.Value, failRetry: (CapacityType.Value, BenchmarkRequest) => Unit) extends ChannelDuplexHandler {
  val workingRequests: mutable.LongMap[BenchmarkRequest] = mutable.LongMap()

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    msg match {
      case req: BenchmarkRequest =>
        req.byteBuf.retain()
        workingRequests(req.requestId) = req
        ctx.writeAndFlush(req.byteBuf, ctx.voidPromise())
      case any =>
        ReferenceCountUtil.release(any)
    }
  }

  //  override def channelActive(ctx: ChannelHandlerContext): Unit = {
  //    ctx.channel().eventLoop().scheduleAtFixedRate({ () =>
  //      println(s"avg latency = $latencyAverge ms, total = $total")
  //    }, 30, 1, TimeUnit.SECONDS)
  //  }

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
                  val channel = req.replyTo
                  channel.writeAndFlush(BenchmarkResponse.toHttpResponse(buf), channel.voidPromise())

                  ReferenceCountUtil.release(req.byteBuf)
                } else {
                  failRetry(cap, req)
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
