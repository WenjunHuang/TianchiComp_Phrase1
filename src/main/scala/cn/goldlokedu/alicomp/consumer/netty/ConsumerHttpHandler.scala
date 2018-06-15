package cn.goldlokedu.alicomp.consumer.netty

import java.util.UUID

import cn.goldlokedu.alicomp.documents.BenchmarkRequest
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.multipart.{DefaultHttpDataFactory, HttpData, HttpPostStandardRequestDecoder}
import io.netty.util.{CharsetUtil, ReferenceCountUtil}

class ConsumerHttpHandler(sender: (ByteBuf, Long, Channel) => Unit) extends ChannelInboundHandlerAdapter {

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    msg match {
      case req: FullHttpRequest if req.method() == HttpMethod.POST =>
        ctx.channel().eventLoop().execute { () =>
          val requestId = UUID.randomUUID().getLeastSignificantBits
          val decoder = new HttpPostStandardRequestDecoder(new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE), req, CharsetUtil.UTF_8)
          val interface = decoder.getBodyHttpData("interface").asInstanceOf[HttpData].getString
          val method = decoder.getBodyHttpData("method").asInstanceOf[HttpData].getString
          val pts = decoder.getBodyHttpData("parameterTypesString").asInstanceOf[HttpData].getString
          val param = decoder.getBodyHttpData("parameter").asInstanceOf[HttpData].getString
          decoder.destroy()

          val builder = req.content()
          builder.retain()
          builder.resetReaderIndex()
          builder.resetWriterIndex()

          BenchmarkRequest.makeDubboRequest(
            requestId = requestId,
            interface = interface,
            method = method,
            parameterTypeString = pts,
            parameter = param,
            builder
          )

          sender(builder, requestId, ctx.channel())
        }
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
