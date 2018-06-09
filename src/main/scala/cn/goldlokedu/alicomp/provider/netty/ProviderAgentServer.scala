package cn.goldlokedu.alicomp.provider.netty


import java.net.{InetSocketAddress, SocketAddress}

import cn.goldlokedu.alicomp.documents.{CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.{AdaptiveRecvByteBufAllocator, ChannelInitializer, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits._

class ProviderAgentServer(serverHost: String,
                          cap: CapacityType.Value,
                          name: String,
                          dubboHost: String,
                          dubboPort: Int)(implicit etcdClient: EtcdClient, log: Logger) {
  val bossGroup = new NioEventLoopGroup(1)
  implicit val alloc = PooledByteBufAllocator.DEFAULT
  //  val workerGroup = new NioEventLoopGroup(1)

  def run(): Unit = {
    val b = new ServerBootstrap()
      .channel(classOf[NioServerSocketChannel])
      .group(bossGroup)
      .option(ChannelOption.ALLOCATOR, alloc)
      .childOption(ChannelOption.ALLOCATOR,alloc)
      .childOption(ChannelOption.RCVBUF_ALLOCATOR,AdaptiveRecvByteBufAllocator.DEFAULT)
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline().addLast(new ProviderAgentHandler(dubboHost, dubboPort))
        }
      })
    val boundAddress = b
      .bind(new InetSocketAddress(serverHost, 0))
      .sync()
      .channel()
      .localAddress().asInstanceOf[InetSocketAddress]
    etcdClient.addProvider(RegisteredAgent(cap, name, boundAddress.getHostString, boundAddress.getPort))
      .onComplete(_ => etcdClient.shutdown())
  }

}
