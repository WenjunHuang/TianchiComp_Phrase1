package cn.goldlokedu.alicomp.consumer

import java.lang
import java.net.InetSocketAddress
import java.util.concurrent.ThreadLocalRandom

import cn.goldlokedu.alicomp.consumer.netty.{ConsumerHttpHandler, ProviderAgentHandler}
import cn.goldlokedu.alicomp.documents.{CapacityType, _}
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
  val bossGroup = ServerUtils.newGroup(2)
  val workerGroup = bossGroup
  var providerAgents: mutable.Map[CapacityType.Value, mutable.Buffer[Channel]] = mutable.Map()
  var serverChannel: Channel = _

  val MaxRoll = 13
  val largeBound = Set(0, 1, 3, 4, 5, 9, 10)
  val mediumBound = Set(2, 6, 7, 11, 12)
  val smallBound = Set(8)
  val connectionCount = Map(CapacityType.L -> 2, CapacityType.M -> 2, CapacityType.S -> 2)

  private def failRetry(cap: CapacityType.Value, req: BenchmarkRequest) = {
    cap match {
      case CapacityType.L =>
        callProviderAgent(CapacityType.M, req)
      case CapacityType.M =>
        callProviderAgent(CapacityType.S, req)
      case CapacityType.S =>
        req.replyTo.writeAndFlush(BenchmarkResponse.errorHttpResponse)

    }
  }

  private def connectProviderAgents() = {
    etcdClient.providers()
      .map { ras =>
        val rest = ras.filterNot(p => providerAgents.contains(p.cap))
        rest.foreach { agent =>
          println(s"connecting $agent")
          (0 until connectionCount(agent.cap)).foreach { _ =>
            val b1 = createProviderAgentBootstrap(agent.cap, failRetry)
            b1.connect(new InetSocketAddress(agent.host, agent.port))
              .addListener { future: ChannelFuture =>
                if (future.isSuccess) {
                  println(s"connected $agent")
                  val ch = future.channel()
                  serverChannel.eventLoop().execute(() => {
                    providerAgents.getOrElseUpdate(agent.cap, mutable.Buffer[Channel]()).+=(ch)
                  })
                } else {
                  future.cause().printStackTrace()
                }
              }
          }
        }
      }
      .onComplete(_ => etcdClient.shutdown())
  }

  private def createProviderAgentBootstrap(cap: CapacityType.Value, failRetry: (CapacityType.Value, BenchmarkRequest) => Unit) = {
    val b = new Bootstrap
    b.group(workerGroup)
      .option[lang.Boolean](ChannelOption.TCP_NODELAY, true)
      .option[lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option[java.lang.Integer](ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
      .option[java.lang.Integer](ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
      .handler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          ch.pipeline().addFirst(new LengthFieldBasedFrameDecoder(1024, DubboMessage.HeaderSize, 4))
          ch.pipeline().addLast(new ProviderAgentHandler(cap, failRetry))
        }
      })
    ServerUtils.setChannelClass(b)
    b
  }


  def run() = {
    val bootstrap = new ServerBootstrap()
    bootstrap.group(bossGroup, workerGroup)
      .option[java.lang.Integer](ChannelOption.SO_BACKLOG, 1024)
      .option[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .handler(new LoggingHandler(LogLevel.INFO))
      .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .childOption[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .childOption[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
      .childOption[java.lang.Boolean](ChannelOption.SO_REUSEADDR, true)
      .childOption[java.lang.Integer](ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
      .childOption[java.lang.Integer](ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
      .childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(2 * 1024))
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast("codec", new HttpServerCodec())
          pipeline.addLast("aggregator", new HttpObjectAggregator(2 * 1024))
          pipeline.addLast("handler", new ConsumerHttpHandler({ (byteBuf, requestId, channel) =>
            val roll = ThreadLocalRandom.current().nextInt(MaxRoll)
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
            val req = BenchmarkRequest(byteBuf, requestId, channel)
            callProviderAgent(cap, req)
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

  private def callProviderAgent(cap: CapacityType.Value, req: BenchmarkRequest) = {
    val chs = providerAgents(cap)
    val i = ThreadLocalRandom.current().nextInt(chs.size)
    val ch = chs(i)
    ch.writeAndFlush(req, ch.voidPromise())
  }
}
