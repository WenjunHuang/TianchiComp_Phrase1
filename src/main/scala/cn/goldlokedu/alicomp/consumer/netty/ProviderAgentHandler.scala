package cn.goldlokedu.alicomp.consumer.netty

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, ChannelInboundHandlerAdapter}

class ProviderAgentHandler(responder: (ByteBuf) => Unit) extends ChannelInboundHandlerAdapter {
  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        responder(buf)
    }
  }
}
