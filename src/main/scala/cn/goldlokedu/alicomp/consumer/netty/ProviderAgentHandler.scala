package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessage}
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable

class ProviderAgentHandler extends ChannelDuplexHandler {
  val workingRequests: mutable.LongMap[Channel] = mutable.LongMap()

  override def write(ctx: ChannelHandlerContext, msg: scala.Any, promise: ChannelPromise): Unit = {
    msg match {
      case req: BenchmarkRequest =>
        workingRequests(req.requestId) = req.replyTo
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
          isEvent <- DubboMessage.extractIsEvent(buf) if !isEvent
          requestId <- DubboMessage.extractRequestId(buf)
        } {
          workingRequests.remove(requestId) match {
            case Some(channel) =>
              channel.eventLoop().execute { () =>
                channel.writeAndFlush(BenchmarkResponse.toHttpResponse(buf), channel.voidPromise())
              }
            case None =>
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
