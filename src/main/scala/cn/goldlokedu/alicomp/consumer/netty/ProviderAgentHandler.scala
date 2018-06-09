package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.DubboMessageBuilder
import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

class ProviderAgentHandler(responder: (Seq[ByteBuf]) => Unit)(implicit alloc: ByteBufAllocator) extends ChannelInboundHandlerAdapter {
  var messageBuilder: DubboMessageBuilder = DubboMessageBuilder(alloc.compositeDirectBuffer(), alloc)

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        val result = messageBuilder.feedRaw(buf)
        messageBuilder = result._1
        if (result._2.nonEmpty)
          responder(result._2)
      case _ =>
    }
  }
}
