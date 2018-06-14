package cn.goldlokedu.alicomp.provider.netty

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.Unpooled
import io.netty.channel.epoll._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{Channel, ChannelFutureListener, ChannelOption}

object ServerUtils {
  def closeOnFlush(ch: Channel) = {
    if (ch.isActive)
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
  }

  def setServerChannelClass(bootstrap: ServerBootstrap) = {
    if (Epoll.isAvailable()) {
      bootstrap.option(ChannelOption.SO_REUSEADDR, true)
      bootstrap.channel(classOf[EpollServerSocketChannel])
      bootstrap.option[java.lang.Integer](ChannelOption.IP_TOS, 13)
    }
    else
      bootstrap.channel(classOf[NioServerSocketChannel])
  }

  def setChannelClass(bootstrap: Bootstrap) = {
    if (Epoll.isAvailable()) {
      bootstrap.channel(classOf[EpollSocketChannel])
      bootstrap.option(ChannelOption.SO_REUSEADDR, true)
      bootstrap.option[java.lang.Integer](ChannelOption.IP_TOS, 13)
    }
    else {
      bootstrap.channel(classOf[NioSocketChannel])
    }
  }

  def newGroup(threads: Int = 0) = {
    if (Epoll.isAvailable()) {
      val eg = new EpollEventLoopGroup(threads)
      //      eg.setIoRatio(100)
      eg
    }
    else {
      val ng = new NioEventLoopGroup(threads)
      //      ng.setIoRatio(100)
      ng
    }
  }
}
