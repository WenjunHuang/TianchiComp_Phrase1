package cn.goldlokedu.alicomp.provider.netty

import cn.goldlokedu.alicomp.documents.DubboMessage
import cn.goldlokedu.alicomp.util.{DirectClientHandler, RelayHandler}
import io.netty.bootstrap.Bootstrap
import io.netty.channel._
import io.netty.handler.codec.LengthFieldBasedFrameDecoder
import io.netty.util.concurrent.Future
import org.slf4j.Logger

class ProviderAgentInitHandler(dubboHost: String,
                               dubboPort: Int)(implicit log: Logger) extends ChannelInboundHandlerAdapter {

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    val promise = ctx.executor().newPromise[Channel]()
    promise.addListener { future: Future[Channel] =>
      val outboundChannel = future.getNow
      if (future.isSuccess) {
        ctx.pipeline().remove(ProviderAgentInitHandler.this)
        outboundChannel.pipeline().addFirst(new LengthFieldBasedFrameDecoder(2 * 1024, DubboMessage.HeaderSize, 4))
        outboundChannel.pipeline().addLast(new DubboToProtocalHandler)
        outboundChannel.pipeline().addLast(new RelayHandler(ctx.channel()))
        ctx.pipeline().addFirst(new LengthFieldBasedFrameDecoder(2 * 1024, 8, 4))
        ctx.pipeline().addLast(new ProtocalHandler)
        ctx.pipeline().addLast(new RelayHandler(outboundChannel))
      } else {
        ServerUtils.closeOnFlush(ctx.channel())
      }
    }

    val b = new Bootstrap()
      .group(ctx.channel().eventLoop())
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