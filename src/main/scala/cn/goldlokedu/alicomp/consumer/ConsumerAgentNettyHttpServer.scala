package cn.goldlokedu.alicomp.consumer

import java.net.InetSocketAddress
import java.util.concurrent.ThreadLocalRandom

import cn.goldlokedu.alicomp.consumer.netty.{ConsumerHttpHandler, ProviderAgentHandler}
import cn.goldlokedu.alicomp.documents._
import cn.goldlokedu.alicomp.etcd.EtcdClient
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel._
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.{NioServerSocketChannel, NioSocketChannel}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._

class ConsumerAgentNettyHttpServer(etcdClient: EtcdClient,
                                   consumerHttpHost: String,
                                   consumerHttpPort: Int) {
  val bossGroup = new NioEventLoopGroup(1)
  val workerGroup = new NioEventLoopGroup()
  implicit val alloc = PooledByteBufAllocator.DEFAULT
  var providerAgents: mutable.Map[CapacityType.Value, Channel] = mutable.Map.empty
  var serverChannel: Channel = _

  private def connectProviderAgents() = {
    etcdClient.providers()
      .map { ras =>
        val rest = ras.filterNot(p => providerAgents.contains(p.cap))
        rest.foreach { agent =>
          println(s"connecting $agent")
          val b = new Bootstrap
          b.group(workerGroup)
            .option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
            .option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
            .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.RCVBUF_ALLOCATOR,AdaptiveRecvByteBufAllocator.DEFAULT)
            .option(ChannelOption.ALLOCATOR,alloc)
            .channel(classOf[NioSocketChannel])
            .handler(new ProviderAgentHandler)
            .connect(new InetSocketAddress(agent.host, agent.port))
            .addListener { future: ChannelFuture =>
              if (future.isSuccess) {
                println(s"connected $agent")
                val ch = future.channel()
                serverChannel.eventLoop().execute(() => {
                  providerAgents(agent.cap) = ch
                  println(providerAgents)
                })
              } else {
                future.cause().printStackTrace()
              }
            }
        }
      }
      .onComplete(_ => etcdClient.shutdown())
  }

  def run() = {
    val bootstrap = new ServerBootstrap()
    bootstrap.option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
    bootstrap.group(bossGroup,workerGroup)
      .option(ChannelOption.ALLOCATOR, alloc)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childOption(ChannelOption.ALLOCATOR, alloc)
      .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast("codec", new HttpServerCodec())
          pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024))
          pipeline.addLast("handler", new ConsumerHttpHandler({ (byteBuf, requestId, channel) =>
            val roll = ThreadLocalRandom.current().nextInt(12)
            val cap = roll match {
              case x if Seq(0, 2, 4, 6, 8, 10) contains x =>
                CapacityType.L
              case x if Seq(1, 3, 5, 7) contains x =>
                CapacityType.M
              case 9 =>
                CapacityType.S
              case _ =>
                CapacityType.L
            }
            val ch = providerAgents.getOrElse(cap, providerAgents.headOption.map(_._2).get)
            ch.pipeline().fireUserEventTriggered(BenchmarkRequest(byteBuf, requestId, channel))
          }))
        }
      })

    serverChannel = bootstrap.bind(consumerHttpPort)
      .sync().channel()

    connectProviderAgents()
    serverChannel.closeFuture().sync()
    workerGroup.shutdownGracefully()
  }

}
