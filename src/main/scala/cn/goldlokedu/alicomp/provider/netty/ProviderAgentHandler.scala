package cn.goldlokedu.alicomp.provider.netty

import cn.goldlokedu.alicomp.util.{DirectClientHandler, RelayHandler}
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel._
import io.netty.util.concurrent.Future
import org.slf4j.Logger

class ProviderAgentHandler(dubboHost: String,
                           dubboPort: Int)(implicit log: Logger) extends ChannelInboundHandlerAdapter {

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

    val b = new Bootstrap()
      .group(ctx.channel().eventLoop())
      .option[java.lang.Boolean](ChannelOption.SO_KEEPALIVE, true)
      .option[java.lang.Boolean](ChannelOption.TCP_NODELAY, true)
      .option[java.lang.Integer](ChannelOption.SO_SNDBUF, 4 * 1024 * 1024)
      .option[java.lang.Integer](ChannelOption.SO_RCVBUF, 4 * 1024 * 1024)
      .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
      .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
      .handler(new DirectClientHandler(promise))

    ServerUtils.setChannelClass(b)

    b.connect(dubboHost, dubboPort)
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
