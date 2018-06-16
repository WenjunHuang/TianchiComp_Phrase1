package cn.goldlokedu.alicomp.consumer

import java.net.InetSocketAddress
import java.util.concurrent.ForkJoinPool

import cn.goldlokedu.alicomp.consumer.netty.{ConsumerHttpHandler, ProviderAgentHandler, ProviderAgentUtils}
import cn.goldlokedu.alicomp.documents.{CapacityType, _}
import cn.goldlokedu.alicomp.etcd.EtcdClient
import cn.goldlokedu.alicomp.provider.netty.ServerUtils
import io.netty.bootstrap.{Bootstrap, ServerBootstrap}
import io.netty.buffer.ByteBuf
import io.netty.channel._
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.util.ReferenceCountUtil

import scala.collection.convert.ImplicitConversions._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService}

class ConsumerAgentNettyHttpServer(etcdClient: EtcdClient,
                                   consumerHttpHost: String,
                                   consumerHttpPort: Int) {
  val bossGroup = ServerUtils.newGroup(1)
  val workerGroup = bossGroup
  var serverChannel: Channel = _

  private def failRetry(cap: CapacityType.Value, req: BenchmarkRequest) = {
    cap match {
      case CapacityType.L =>
        val agentChannel = ProviderAgentUtils.getProviderAgentChannel(CapacityType.M)
        agentChannel.writeAndFlush(req, agentChannel.voidPromise())
      case CapacityType.M =>
        val agentChannel = ProviderAgentUtils.getProviderAgentChannel(CapacityType.S)
        agentChannel.writeAndFlush(req, agentChannel.voidPromise())
      case CapacityType.S =>
        ReferenceCountUtil.release(req.byteBuf)
        req.replyTo.writeAndFlush(BenchmarkResponse.errorHttpResponse)

    }
  }

  private def connectProviderAgents()(implicit ec:ExecutionContext) = {
    etcdClient.providers()
      .map { ras =>
        ras.foreach { agent =>
          println(s"connecting $agent")
          workerGroup.next()
          for (child <- workerGroup) {
            val eventLoop = child.asInstanceOf[EventLoop]
            eventLoop.execute { () =>
              val b1 = createProviderAgentBootstrap(agent.cap, failRetry, eventLoop)
              b1.connect(new InetSocketAddress(agent.host, agent.port))
                .addListener { future: ChannelFuture =>
                  if (future.isSuccess) {
                    println(s"connected $agent")
                    val ch = future.channel()
                    ProviderAgentUtils.setProviderAgentChannel(agent.cap, ch)
                  } else {
                    future.cause().printStackTrace()
                  }
                }
            }
          }
        }
      }
      .onComplete(_ => etcdClient.shutdown())
  }

  private def createProviderAgentBootstrap(cap: CapacityType.Value,
                                           failRetry: (CapacityType.Value, BenchmarkRequest) => Unit,
                                           group: EventLoopGroup)(implicit ec:ExecutionContext) = {
    val b = new Bootstrap
    b.group(group)
      .handler(new ChannelInitializer[Channel] {
        override def initChannel(ch: Channel): Unit = {
          ch.pipeline().addFirst(new LengthFieldBasedFrameDecoder(2 * 1024, DubboMessage.HeaderSize, 4))
          ch.pipeline().addLast(new ProviderAgentHandler(cap, failRetry))
        }
      })
    ServerUtils.setChannelClass(b)
    b
  }


  def run() = {
    implicit val ec: ExecutionContextExecutorService = ExecutionContext.fromExecutorService(new ForkJoinPool(2))
    val bootstrap = new ServerBootstrap()
    bootstrap.group(bossGroup, workerGroup)
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(new ChannelInitializer[SocketChannel] {
        override def initChannel(ch: SocketChannel): Unit = {
          val pipeline = ch.pipeline()
          pipeline.addLast("codec", new HttpServerCodec())
          pipeline.addLast("aggregator", new HttpObjectAggregator(2 * 1024))
          pipeline.addLast("handler", new ConsumerHttpHandler(chooseAndCallProvider))
        }
      })

    ServerUtils.setServerChannelClass(bootstrap)

    serverChannel = bootstrap.bind(consumerHttpPort)
      .sync().channel()

    connectProviderAgents()
    serverChannel.closeFuture().sync()
    bossGroup.shutdownGracefully()
  }

  private def chooseAndCallProvider(byteBuf: ByteBuf,
                                    requestId: Long,
                                    channel: Channel) = {
    val agentChannel = ProviderAgentUtils.chooseProviderAgent()
    val req = BenchmarkRequest(byteBuf, requestId, channel)
    agentChannel.writeAndFlush(req, agentChannel.voidPromise())
  }
}
