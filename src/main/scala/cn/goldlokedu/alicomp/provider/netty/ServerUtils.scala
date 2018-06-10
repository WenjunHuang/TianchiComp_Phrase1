package cn.goldlokedu.alicomp.provider.netty

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.Unpooled
import io.netty.channel.epoll.{Epoll, EpollEventLoopGroup, EpollServerSocketChannel, EpollSocketChannel}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel.{Channel, ChannelFutureListener}

object ServerUtils {
  def closeOnFlush(ch: Channel) = {
    if (ch.isActive)
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
  }

  def setServerChannelClass(bootstrap: ServerBootstrap) = {
    if (Epoll.isAvailable())
      bootstrap.channel(classOf[EpollServerSocketChannel])
    else
      bootstrap.channel(classOf[NioServerSocketChannel])
  }

  def setChannelClass(bootstrap: Bootstrap) = {
    if (Epoll.isAvailable())
      bootstrap.channel(classOf[EpollSocketChannel])
    else
      bootstrap.channel(classOf[NioSocketChannel])
  }

  def newGroup(threads: Int = 0) = {
    if (Epoll.isAvailable()) new EpollEventLoopGroup(threads) else new NioEventLoopGroup(threads)
  }
}
