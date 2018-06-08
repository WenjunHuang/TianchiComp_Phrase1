package cn.goldlokedu.alicomp.consumer.netty

import java.util.UUID

import cn.goldlokedu.alicomp.documents.BenchmarkRequest
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.multipart.{DefaultHttpDataFactory, HttpData, HttpPostRequestDecoder}

class ConsumerHttpHandler(sender: (ByteBuf, Long, Channel) => Unit) extends SimpleChannelInboundHandler[FullHttpRequest] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    msg.method match {
      case HttpMethod.POST =>
        val decoder = new HttpPostRequestDecoder(msg)
        val interface = decoder.getBodyHttpData("interface").asInstanceOf[HttpData].getString
        val method = decoder.getBodyHttpData("method").asInstanceOf[HttpData].getString
        val pts = decoder.getBodyHttpData("parameterTypesString").asInstanceOf[HttpData].getString
        val param = decoder.getBodyHttpData("parameter").asInstanceOf[HttpData].getString
        decoder.destroy()

        val requestId = UUID.randomUUID().getLeastSignificantBits
        val byteBuf = BenchmarkRequest.toDubboRequestByteBuf(BenchmarkRequest(
          requestId = requestId,
          interface = interface,
          method = method,
          parameterTypeString = pts,
          parameter = param
        ))(ctx.alloc())

        sender(byteBuf, requestId, ctx.channel())
      case _ =>
        ctx.close()
    }
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }

  override def exceptionCaught(ctx: _root_.io.netty.channel.ChannelHandlerContext, cause: _root_.java.lang.Throwable): Unit = {
    cause.printStackTrace()
  }
}

object ConsumerHttpHandler {
  val Factory = new DefaultHttpDataFactory()
}
