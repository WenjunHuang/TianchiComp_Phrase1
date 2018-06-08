package cn.goldlokedu.alicomp.consumer

import java.net.InetSocketAddress

import cn.goldlokedu.alicomp.consumer.netty.ConsumerHttpServerInitializer
import cn.goldlokedu.alicomp.documents.CapacityType
import cn.goldlokedu.alicomp.etcd.EtcdClient
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{Channel, ChannelFuture, ChannelOption}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

class ConsumerAgentNettyHttpServer(etcdClient: EtcdClient,
                                   consumerHttpHost: String,
                                   consumerHttpPort: Int) {
  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup(1)
  var providerAgents: Map[CapacityType.Value, Channel] = Map.empty

  private def getProviderAgents(ch: Channel) = {
    etcdClient.providers()
      .map { ras =>
        val rest = ras.filterNot(p => providerAgents.contains(p.cap))
        rest.foreach { agent =>
          val b = new Bootstrap
          b.group(bossGroup)
            .handler(new ProviderAgentClientInitializer)
            .connect(new InetSocketAddress(agent.host, agent.port))
            .addListener { future: ChannelFuture =>
              ch.eventLoop().execute(() => {
                providerAgents = providerAgents + (agent.cap -> future.channel())
              })
            }
        }
      }
      .onComplete(_ => etcdClient.shutdown())
  }

  def run() = {
    val bootstrap = new ServerBootstrap()
    bootstrap.option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
    bootstrap.group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new ConsumerHttpServerInitializer)

    val ch = bootstrap.bind(consumerHttpPort)
      .sync().channel()

    getProviderAgents(ch)

    ch.closeFuture().sync()
    bossGroup.shutdownGracefully()
    workerGroup.shutdownGracefully()
  }

}
