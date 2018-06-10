package cn.goldlokedu.alicomp.consumer

import java.net.InetSocketAddress
import java.util.concurrent.ThreadLocalRandom

import cn.goldlokedu.alicomp.consumer.netty.{ConsumerHttpHandler, ProviderAgentHandler}
import cn.goldlokedu.alicomp.documents._
import cn.goldlokedu.alicomp.etcd.EtcdClient
import cn.goldlokedu.alicomp.provider.netty.ServerUtils
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.logging.{LogLevel, LoggingHandler}

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits._

class ConsumerAgentNettyHttpServer(etcdClient: EtcdClient,
                                   consumerHttpHost: String,
                                   consumerHttpPort: Int) {
  val bossGroup = ServerUtils.newGroup(1)
  val workerGroup = ServerUtils.newGroup(1)
  val agentGroup = ServerUtils.newGroup(1)
  var providerAgents: mutable.Map[CapacityType.Value, Channel] = mutable.Map.empty
  var serverChannel: Channel = _
  val largeBound = Seq(0, 2, 4, 6, 8, 10, 11, 7)
  val mediumBound = Seq(1, 3, 5)
  val smallBound = Seq(9)

  private def connectProviderAgents() = {
    etcdClient.providers()
      .map { ras =>
        val rest = ras.filterNot(p => providerAgents.contains(p.cap))
        rest.foreach { agent =>
          println(s"connecting $agent")
          val b = new Bootstrap
          b.group(agentGroup)
            .option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
            .option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
            .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
            .option[java.lang.Integer](ChannelOption.MAX_MESSAGES_PER_READ, 128)
            .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
            .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
            .handler(new ChannelInitializer[Channel] {
              override def initChannel(ch: Channel): Unit = {
                ch.pipeline().addFirst(new LengthFieldBasedFrameDecoder(1024, DubboMessage.HeaderSize, 4))
                ch.pipeline().addLast(new ProviderAgentHandler)
              }
            })
          ServerUtils.setChannelClass(b)

          b.connect(new InetSocketAddress(agent.host, agent.port))
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
    bootstrap.group(bossGroup, workerGroup)
      .option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
      .option[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
      .option[Integer](ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
      .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .handler(new LoggingHandler(LogLevel.INFO))
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
      .childOption[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
      .childOption[Integer](ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast("codec", new HttpServerCodec())
          pipeline.addLast("aggregator", new HttpObjectAggregator(512 * 1024))
          pipeline.addLast("handler", new ConsumerHttpHandler({ (byteBuf, requestId, channel) =>
            val roll = ThreadLocalRandom.current().nextInt(12)
            val cap = roll match {
              case x if largeBound contains x =>
                CapacityType.L
              case x if mediumBound contains x =>
                CapacityType.M
              case x if smallBound contains x =>
                CapacityType.S
              case _ =>
                CapacityType.L
            }
            val ch = providerAgents.getOrElse(cap, providerAgents.headOption.map(_._2).get)
            ch.pipeline().fireUserEventTriggered(BenchmarkRequest(byteBuf, requestId, channel))
          }))
        }
      })

    ServerUtils.setServerChannelClass(bootstrap)

    serverChannel = bootstrap.bind(consumerHttpPort)
      .sync().channel()

    connectProviderAgents()
    serverChannel.closeFuture().sync()
    bossGroup.shutdownGracefully()
  }

}
