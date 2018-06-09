package cn.goldlokedu.alicomp.provider.netty


import java.net.InetSocketAddress

import cn.goldlokedu.alicomp.documents.{CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.epoll.{EpollEventLoopGroup, EpollServerSocketChannel}
import io.netty.channel.socket.SocketChannel
import io.netty.channel.{AdaptiveRecvByteBufAllocator, ChannelInitializer, ChannelOption}
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits._

class ProviderAgentServer(serverHost: String,
                          cap: CapacityType.Value,
                          name: String,
                          dubboHost: String,
                          dubboPort: Int)(implicit etcdClient: EtcdClient, log: Logger) {
  val group = new EpollEventLoopGroup(2)
  implicit val alloc = PooledByteBufAllocator.DEFAULT

  def run(): Unit = {
    val b = new ServerBootstrap()
      .channel(classOf[EpollServerSocketChannel])
      .group(group)
      .option(ChannelOption.ALLOCATOR, alloc)
      .childOption(ChannelOption.ALLOCATOR, alloc)
      .childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline().addLast(new ProviderAgentHandler(dubboHost, dubboPort))
        }
      })
    val serverChannel = b
      .bind(new InetSocketAddress(serverHost, 0))
      .sync()
      .channel()
    val boundAddress = serverChannel.localAddress().asInstanceOf[InetSocketAddress]
    etcdClient.addProvider(RegisteredAgent(cap, name, boundAddress.getHostString, boundAddress.getPort))
      .onComplete(_ => etcdClient.shutdown())


    serverChannel.closeFuture().sync()
    group.shutdownGracefully()
  }

}
