package cn.goldlokedu.alicomp.consumer.netty

import java.util.UUID

import cn.goldlokedu.alicomp.documents.BenchmarkRequest
import io.netty.buffer.{ByteBuf, CompositeByteBuf}
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.multipart.{DefaultHttpDataFactory, HttpData, HttpPostStandardRequestDecoder}
import io.netty.util.{ByteProcessor, CharsetUtil, ReferenceCountUtil}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}

class ConsumerHttpHandler(sender: (ByteBuf, Long, Channel) => Unit)(implicit ec: ExecutionContext) extends ChannelInboundHandlerAdapter {
  val contents: mutable.Buffer[HttpContent] = mutable.Buffer()
  var size = 0

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    msg match {
      case req: HttpRequest =>
        size = req.headers().getInt(HttpHeaderNames.CONTENT_LENGTH)
      case last: LastHttpContent =>
        contents += last
        val cb = ctx.alloc().compositeBuffer(contents.size)
        cb.addComponents(true, contents.map { it =>
          val c = it.content().retain()
          it.release()
          c
        }: _*)
        contents.clear()


        val requestId = UUID.randomUUID().getLeastSignificantBits
        val agentChannel = ProviderAgentUtils.chooseProviderAgent()
        agentChannel.writeAndFlush(BenchmarkRequest(cb, requestId, ctx.channel()), agentChannel.voidPromise())
      case body: HttpContent =>
        contents += body
      case any =>
        ReferenceCountUtil.release(any)
        ctx.close()
    }
  }

  override def exceptionCaught(ctx: _root_.io.netty.channel.ChannelHandlerContext, cause: _root_.java.lang.Throwable): Unit = {
    cause.printStackTrace()
  }
}

object ConsumerHttpHandler {
  val Factory = new DefaultHttpDataFactory()
}
