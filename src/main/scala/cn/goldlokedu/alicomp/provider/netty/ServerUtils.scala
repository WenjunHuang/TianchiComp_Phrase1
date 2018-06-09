package cn.goldlokedu.alicomp.provider.netty

import io.netty.buffer.Unpooled
import io.netty.channel.{Channel, ChannelFutureListener}

object ServerUtils {
  def closeOnFlush(ch:Channel) = {
    if (ch.isActive)
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
  }

}
