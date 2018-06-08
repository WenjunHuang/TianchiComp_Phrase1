package cn.goldlokedu.alicomp.consumer.netty

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{Channel, ChannelInitializer}
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder
import io.netty.handler.codec.http._
import io.netty.handler.logging.LoggingHandler

class ConsumerHttpServerInitializer extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {
    val pipeline = ch.pipeline()
    pipeline.addLast(new LoggingHandler())
    pipeline.addLast("codec",new HttpServerCodec())
    pipeline.addLast("aggregator",new HttpObjectAggregator(512 * 1024))
    pipeline.addLast("handler",new ConsumerHttpHandler)
  }
}
