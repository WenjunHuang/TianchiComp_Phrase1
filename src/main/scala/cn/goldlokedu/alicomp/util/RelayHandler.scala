package cn.goldlokedu.alicomp.util

import cn.goldlokedu.alicomp.provider.netty.ServerUtils
import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

class RelayHandler(relayChannel: Channel) extends ChannelInboundHandlerAdapter {
  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER, ctx.voidPromise())
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    if (relayChannel.isActive)
      relayChannel.writeAndFlush(msg, relayChannel.voidPromise())
    else
      ReferenceCountUtil.release(msg)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    if (relayChannel.isActive)
      relayChannel.flush()
  }

  override def channelInactive(ctx: ChannelHandlerContext): Unit = {
    if (relayChannel.isActive())
      ServerUtils.closeOnFlush(relayChannel)
  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close()
  }

}
