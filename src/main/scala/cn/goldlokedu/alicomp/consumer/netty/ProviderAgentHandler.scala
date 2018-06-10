package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessage}
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable

class ProviderAgentHandler extends ChannelInboundHandlerAdapter {
  val workingRequests: mutable.Map[Long, Channel] = mutable.Map.empty

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
    evt match {
      case req: BenchmarkRequest =>
        workingRequests(req.requestId) = req.replyTo
        ctx.writeAndFlush(req.byteBuf)
      case _ =>
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        for {
          isResponse <- DubboMessage.extractIsResponse(buf) if isResponse
          requestId <- DubboMessage.extractRequestId(buf)
        } {
          workingRequests.remove(requestId) match {
            case Some(channel) =>
              channel.writeAndFlush(BenchmarkResponse.toHttpResponse(buf), channel.voidPromise())
            case None =>
              ReferenceCountUtil.release(msg)
          }
        }
      case _ =>
    }
  }
}
