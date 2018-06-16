package cn.goldlokedu.alicomp.provider.netty


import java.net.InetSocketAddress

import cn.goldlokedu.alicomp.documents.{CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import org.slf4j.Logger

import scala.concurrent.ExecutionContext.Implicits._

class ProviderAgentServer(serverHost: String,
                          cap: CapacityType.Value,
                          name: String,
                          dubboHost: String,
                          dubboPort: Int)(implicit etcdClient: EtcdClient, log: Logger) {
  val bossGroup = ServerUtils.newGroup(2)
  val workerGroup = bossGroup

  def run(): Unit = {
    val b = new ServerBootstrap()
      .group(bossGroup, workerGroup)
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          ch.pipeline().addLast(new ProviderAgentHandler(dubboHost, dubboPort))
        }
      })
    ServerUtils.setServerChannelClass(b)
    val serverChannel = b
      .bind(new InetSocketAddress(serverHost, 0))
      .sync()
      .channel()
    val boundAddress = serverChannel.localAddress().asInstanceOf[InetSocketAddress]
    etcdClient.addProvider(RegisteredAgent(cap, name, boundAddress.getHostString, boundAddress.getPort))
      .onComplete(_ => etcdClient.shutdown())

    serverChannel.closeFuture().sync()
    bossGroup.shutdownGracefully()
  }

}
