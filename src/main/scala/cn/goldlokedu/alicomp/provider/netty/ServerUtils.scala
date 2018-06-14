package cn.goldlokedu.alicomp.provider.netty

import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.{PooledByteBufAllocator, Unpooled}
import io.netty.channel.epoll._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.channel._

object ServerUtils {
  def closeOnFlush(ch: Channel) = {
    if (ch.isActive)
      ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE)
  }

  def setServerChannelClass(bootstrap: ServerBootstrap) = {
    if (Epoll.isAvailable()) {
      bootstrap.option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
        .channel(classOf[EpollServerSocketChannel])
        .option[java.lang.Integer](ChannelOption.IP_TOS, 13)
        .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2 * 1024))
        .childOption[java.lang.Integer](ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
        .childOption[java.lang.Integer](ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
        .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 2 * 1024 * 1024))
    }
    else
      bootstrap.channel(classOf[NioServerSocketChannel])
  }

  def setChannelClass(bootstrap: Bootstrap) = {
    if (Epoll.isAvailable()) {
      bootstrap.channel(classOf[EpollSocketChannel])
      bootstrap.option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
        .option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
        .option[java.lang.Integer](ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
        .option[java.lang.Integer](ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
        .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
        .option(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2 * 1024))
        .option(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(1024 * 1024, 2 * 1024 * 1024))
        .option[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
        .option[java.lang.Integer](ChannelOption.IP_TOS, 13)
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
