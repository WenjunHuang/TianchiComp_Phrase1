package cn.goldlokedu.alicomp.documents


import io.netty.buffer.ByteBuf
import io.netty.channel.Channel

case class BenchmarkRequest(byteBuf: ByteBuf, requestId: Long, replyTo: Channel)

object BenchmarkRequest {
  // fastjson 的字符串需要带上""
  val DubboVersion = "\"2.6.0\""
  val RequestVersion = "\"0.0.0\""
  // magic
  val magic = 0xdabb
  //req + 2way + event + serialization id + status
  val req = 0xc600

  @inline
  def makeDubboRequest(requestId: Long,
                       interface: String,
                       method: String,
                       parameterTypeString: String,
                       parameter: String,
                       builder: ByteBuf) = {
    createDubboRequestHeader(builder, requestId)
    createDubboRequestBody(interface, method, parameterTypeString, parameter, builder)
    builder
  }

  @inline
  private def createDubboRequestBody(interface: String, method: String, parameterTypeString: String, parameter: String, byteBuf: ByteBuf) = {
    val body = DubboVersion + "\n" +
      s""""$interface"""" + "\n" +
      RequestVersion + "\n" +
      s""""$method"""" + "\n" +
      s""""$parameterTypeString"""" + "\n" +
      s""""${parameter}"""" + "\n" +
      s"{}"
    val bytes = body.getBytes("UTF-8")
    byteBuf.writeInt(bytes.size)
    byteBuf.writeBytes(bytes)
  }

  @inline
  private def createDubboRequestHeader(builder: ByteBuf, requestId: Long) = {
    builder.writeShort(magic)
    builder.writeShort(req)
    builder.writeLong(requestId)
  }
}


