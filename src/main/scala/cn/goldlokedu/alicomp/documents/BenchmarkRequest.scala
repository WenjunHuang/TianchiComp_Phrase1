package cn.goldlokedu.alicomp.documents


import io.netty.buffer.{ByteBuf, ByteBufAllocator, PooledByteBufAllocator}
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
      val b = ctx.alloc().buffer(12)
      b.writeLong(req.requestId)
      b.writeInt(req.byteBuf.readableBytes())

      val cb = ctx.alloc().compositeBuffer(2)
      cb.addComponent(true, b)
      cb.addComponent(true, req.byteBuf)
      ctx.writeAndFlush(cb, ctx.voidPromise())
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
    val cb = alloc.compositeDirectBuffer(4)
    val header = alloc.buffer(DubboMessage.HeaderWithLength)

    createDubboRequestHeader(header, requestId)
    header.writeInt(Begin.readableBytes() + body.readableBytes() + End.readableBytes())
    cb.addComponent(true, header)
    cb.addComponent(true, Begin.retainedSlice())
    cb.addComponent(true, body)
    cb.addComponent(true, End.retainedSlice())

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

    val bytes = bodyBuilder.result().getBytes(CharsetUtil.UTF_8)

    val buffer = PooledByteBufAllocator.DEFAULT.buffer(bytes.size)
    buffer.writeBytes(bytes)
  }
  val End = {
    val bodyBuilder = new StringBuilder
    bodyBuilder.append('"')
    bodyBuilder.append('\n')
    bodyBuilder.append(Trail)
    val bytes = bodyBuilder.result().getBytes(CharsetUtil.UTF_8)
    val buffer = PooledByteBufAllocator.DEFAULT.buffer(bytes.size)
    buffer.writeBytes(bytes)
  }
}


