package cn.goldlokedu.alicomp.provider.netty

import cn.goldlokedu.alicomp.util.{DirectClientHandler, RelayHandler}
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.channel._
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.Future
import org.slf4j.Logger

class ProviderAgentHandler(dubboHost: String,
                           dubboPort: Int)(implicit log: Logger,alloc:ByteBufAllocator) extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val promise = ctx.executor().newPromise[Channel]()
    promise.addListener { future: Future[Channel] =>
      val outboundChannel = future.getNow
      if (future.isSuccess) {
        ctx.pipeline().remove(ProviderAgentHandler.this)
        outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()))
        ctx.pipeline().addLast(new RelayHandler(outboundChannel))
      } else {
        ServerUtils.closeOnFlush(ctx.channel())
      }
    }

    new Bootstrap()
      .group(ctx.channel().eventLoop())
      .channel(classOf[NioSocketChannel])
      .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
      .option(ChannelOption.ALLOCATOR,alloc)
      .option(ChannelOption.RCVBUF_ALLOCATOR,AdaptiveRecvByteBufAllocator.DEFAULT)
      .handler(new DirectClientHandler(promise))
      .connect(dubboHost, dubboPort)
      .addListener { future: ChannelFuture =>
        if (future.isSuccess) {
          log.info(s"dubbo connected: $dubboHost, port: $dubboPort")
        }
        else {
          ServerUtils.closeOnFlush(ctx.channel())
        }
      }
  }
}
