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
          val content = it.content.retain()
          it.release()
          content
        }: _*)
        contents.clear()


        var resolveName = true
        var nameStart = 0
        var nameEnd = 0
        var valueStart = 0
        var valueEnd = 0
        var index = 0
        var newName: ByteBuf = null
        val params = mutable.Map[String, ByteBuf]()

        cb.forEachByte((value: Byte) => {
          index += 1
          if (resolveName) {
            if (value == '='.toByte) {
              valueStart = index
              valueEnd = valueStart

              newName = cb.slice(nameStart, nameEnd - nameStart)
              resolveName = false
            } else {
              nameEnd += 1
            }
          } else {
            if (value == '&'.toByte) {
              nameStart = index
              nameEnd = nameStart
              val newValue = cb.slice(valueStart, valueEnd - valueStart)
              val name = newName.toString(CharsetUtil.UTF_8)
              params(name) = newValue
              resolveName = true
            } else {
              valueEnd += 1
            }
          }

          true
        })
        params(newName.toString(CharsetUtil.UTF_8)) = cb.slice(valueStart, valueEnd - valueStart)

        val requestId = UUID.randomUUID().getLeastSignificantBits
        val buffer = BenchmarkRequest.makeDubboRequest(requestId, params.get("parameter").get.retain(), ctx.alloc())
        val agentChannel = ProviderAgentUtils.chooseProviderAgent()
        agentChannel.writeAndFlush(BenchmarkRequest(buffer, requestId, ctx.channel()), agentChannel.voidPromise())

        ReferenceCountUtil.release(cb)
      case body: HttpContent =>
        contents += body
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
