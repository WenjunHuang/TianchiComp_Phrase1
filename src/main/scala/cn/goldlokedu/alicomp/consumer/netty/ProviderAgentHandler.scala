package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents._
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable

class ProviderAgentHandler(cap: CapacityType.Value, failRetry: (CapacityType.Value, BenchmarkRequest) => Unit) extends ChannelDuplexHandler {
  val workingRequests: mutable.LongMap[BenchmarkRequest] = mutable.LongMap()

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    msg match {
      case req: BenchmarkRequest =>
        workingRequests(req.requestId) = req
        req.byteBuf.retain()
        BenchmarkRequest.writePrivateRequest(ctx, req)
      case any =>
        ReferenceCountUtil.release(any)
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        val requestId = buf.readLong()
        val status = buf.readShort()
        val result = buf.readInt()
        workingRequests.remove(requestId) match {
          case Some(req) =>
            if (status == 20) {
              ReferenceCountUtil.release(req.byteBuf)
              val channel = req.replyTo
              channel.writeAndFlush(BenchmarkResponse.toHttpResponse(result), channel.voidPromise())
            } else {
              ctx.executor().execute { () =>
                failRetry(cap, req)
              }
            }
          case None =>
        }
      case _ =>
    }
    ReferenceCountUtil.release(msg)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }
}
