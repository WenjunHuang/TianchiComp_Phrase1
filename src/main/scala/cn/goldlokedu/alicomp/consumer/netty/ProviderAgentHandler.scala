package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessage, DubboMessageBuilder}
import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}

import scala.collection.mutable

class ProviderAgentHandler(implicit alloc: ByteBufAllocator) extends ChannelInboundHandlerAdapter {
  var messageBuilder: DubboMessageBuilder = DubboMessageBuilder(alloc.compositeDirectBuffer(), alloc)
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
        val result = messageBuilder.feedRaw(buf)
        messageBuilder = result._1
        if (result._2.nonEmpty) {
          result._2.foreach { b =>
            for {
              isResponse <- DubboMessage.extractIsResponse(b) if isResponse
              requestId <- DubboMessage.extractRequestId(b)
            } {
              workingRequests.remove(requestId) match {
                case Some(channel) =>
                  channel.writeAndFlush(BenchmarkResponse.toHttpResponse(b), channel.voidPromise())
                case None =>
              }
            }
          }
        }
      case _ =>
    }
  }
}
