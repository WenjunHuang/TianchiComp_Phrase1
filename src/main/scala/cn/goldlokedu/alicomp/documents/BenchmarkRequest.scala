package cn.goldlokedu.alicomp.documents


import io.netty.buffer.{ByteBuf, ByteBufAllocator, Unpooled}
import io.netty.channel.{Channel, ChannelHandlerContext}
import io.netty.util.CharsetUtil


case class BenchmarkRequest(byteBuf: ByteBuf, requestId: Long, replyTo: Channel)

object BenchmarkRequest {
  // fastjson 的字符串需要带上""
  val DubboVersion = "\"2.6.0\""
  val RequestVersion = "\"0.0.0\""
  val Trail = "{}"

  // magic
  val magic = 0xdabb
  //req + 2way + event + serialization id + status
  val req = 0xc600

  def writePrivateRequest(ctx: ChannelHandlerContext, req: BenchmarkRequest) = {
    ctx.channel().eventLoop().execute { () =>
      ctx.write(Unpooled.copyLong(req.requestId), ctx.voidPromise())
      ctx.write(Unpooled.copyInt(req.byteBuf.readableBytes()), ctx.voidPromise())
      ctx.write(req.byteBuf, ctx.voidPromise())
      ctx.flush()
    }
  }

  @inline
  def makeDubboRequest(requestId: Long,
                       interface: String,
                       method: String,
                       parameterTypeString: String,
                       parameter: String,
                       builder: ByteBuf) = {
    createDubboRequestHeader(builder, requestId)
    createDubboRequestBody(interface, method, parameterTypeString, parameter, builder)
  }

  def makeDubboRequest(requestId: Long, body: ByteBuf, alloc: ByteBufAllocator) = {
    val cb = alloc.compositeBuffer(3)
    val header = alloc.buffer(DubboMessage.HeaderWithLength)

    createDubboRequestHeader(header, requestId)
    header.writeInt(Begin.length + body.readableBytes() + End.length)
    cb.addComponent(true, header)
    cb.addComponent(true, Unpooled.wrappedBuffer(Begin))
    cb.addComponent(true, body)
    cb.addComponent(true, Unpooled.wrappedBuffer(End))

    cb
  }

  @inline
  private def createDubboRequestBody(interface: String, method: String, parameterTypeString: String, parameter: String, byteBuf: ByteBuf) = {
    val bodyBuilder = new StringBuilder
    bodyBuilder.append(DubboVersion)
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append(interface)
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append(RequestVersion)
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append(method)
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append(parameterTypeString)
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append(parameter)
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append(Trail)

    val bytes = bodyBuilder.result().getBytes("UTF-8")
    byteBuf.writeInt(bytes.size)
    byteBuf.writeBytes(bytes)
  }

  @inline
  private def createDubboRequestHeader(builder: ByteBuf, requestId: Long) = {
    builder.writeShort(magic)
    builder.writeShort(req)
    builder.writeLong(requestId)
  }

  val Begin = {
    val bodyBuilder = new StringBuilder
    bodyBuilder.append(DubboVersion)
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append("com.alibaba.dubbo.performance.demo.provider.IHelloService")
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append(RequestVersion)
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append("hash")
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append('"')
    bodyBuilder.append("Ljava/lang/String;")
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append('"')

    bodyBuilder.result().getBytes(CharsetUtil.UTF_8)
  }
  val End = {
    val bodyBuilder = new StringBuilder
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append(Trail)
    bodyBuilder.result().getBytes(CharsetUtil.UTF_8)
  }
}


