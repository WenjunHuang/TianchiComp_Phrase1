package cn.goldlokedu.alicomp.consumer.netty

import java.util.UUID

import cn.goldlokedu.alicomp.documents.BenchmarkRequest
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.handler.codec.http._
import io.netty.handler.codec.http.multipart.{DefaultHttpDataFactory, HttpData, HttpPostStandardRequestDecoder}
import io.netty.util.{CharsetUtil, ReferenceCountUtil}

import scala.concurrent.{ExecutionContext, Future}

class ConsumerHttpHandler(sender: (ByteBuf, Long, Channel) => Unit)(implicit ec: ExecutionContext) extends ChannelInboundHandlerAdapter {
  var decoder: HttpPostStandardRequestDecoder = _

  override def channelRead(ctx: ChannelHandlerContext, msg: Any): Unit = {

    msg match {
      case req: HttpRequest =>
        decoder = new HttpPostStandardRequestDecoder(new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE), req, CharsetUtil.UTF_8)
      case last: LastHttpContent =>
        decoder.offer(last)
        ReferenceCountUtil.release(last)
        val d = decoder
        decoder = null
        val agentChannel = ProviderAgentUtils.chooseProviderAgent()
        Future {
          val requestId = UUID.randomUUID().getLeastSignificantBits
          val interface = d.getBodyHttpData("interface").asInstanceOf[HttpData].getString
          val method = d.getBodyHttpData("method").asInstanceOf[HttpData].getString
          val pts = d.getBodyHttpData("parameterTypesString").asInstanceOf[HttpData].getString
          val param = d.getBodyHttpData("parameter").asInstanceOf[HttpData].getString
          d.destroy()

          val builder = ctx.alloc().buffer(2 * 1024)
          //          builder.resetReaderIndex()
          //          builder.resetWriterIndex()

          BenchmarkRequest.makeDubboRequest(
            requestId = requestId,
            interface = interface,
            method = method,
            parameterTypeString = pts,
            parameter = param,
            builder
          )
          agentChannel.writeAndFlush(BenchmarkRequest(builder, requestId, ctx.channel()), agentChannel.voidPromise())
        }
      case body: HttpContent =>
        decoder.offer(body)
        ReferenceCountUtil.release(body)
      case req: FullHttpRequest if req.method() == HttpMethod.POST =>
        val agentChannel = ProviderAgentUtils.chooseProviderAgent()
        Future {
          val requestId = UUID.randomUUID().getLeastSignificantBits
          val decoder = new HttpPostStandardRequestDecoder(new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE), req, CharsetUtil.UTF_8)
          val interface = decoder.getBodyHttpData("interface").asInstanceOf[HttpData].getString
          val method = decoder.getBodyHttpData("method").asInstanceOf[HttpData].getString
          val pts = decoder.getBodyHttpData("parameterTypesString").asInstanceOf[HttpData].getString
          val param = decoder.getBodyHttpData("parameter").asInstanceOf[HttpData].getString
          decoder.destroy()

          val builder = req.content()
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

          agentChannel.writeAndFlush(BenchmarkRequest(builder, requestId, ctx.channel()), agentChannel.voidPromise())
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
