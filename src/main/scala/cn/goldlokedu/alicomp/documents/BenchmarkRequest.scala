package cn.goldlokedu.alicomp.documents


import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.channel.Channel

case class BenchmarkRequest(byteBuf:ByteBuf,requestId:Long,replyTo:Channel)

object BenchmarkRequest {
  // fastjson 的字符串需要带上""
  val DubboVersion = "\"2.6.0\""
  val RequestVersion = "\"0.0.0\""


  @inline
  def makeDubboRequest(requestId: Long,
                       interface: String,
                       method: String,
                       parameterTypeString: String,
                       parameter: String)(implicit alloc: ByteBufAllocator): ByteBuf = {
    val builder = alloc.buffer(128)
    createDubboRequestHeader(builder, requestId)
    createDubboRequestBody(interface, method, parameterTypeString, parameter, builder)
    builder
  }

  @inline
  private def createDubboRequestBody(interface: String, method: String, parameterTypeString: String, parameter: String, byteBuf: ByteBuf) = {
    val body = Seq(DubboVersion, // dubbo version
      s""""$interface"""", // service name
      RequestVersion, // service version
      s""""$method"""", // method name
      s""""${parameterTypeString}"""", // method parameter type
      s""""${parameter}"""", // method arguments
      s"{}"
    ).mkString("\n")
    val bytes = body.getBytes("UTF-8")
    byteBuf.writeInt(bytes.size)
    byteBuf.writeBytes(bytes)
  }

  @inline
  private def createDubboRequestHeader(builder: ByteBuf, requestId: Long) = {
    // magic
    val magic = 0xdabb
    //req + 2way + event + serialization id + status
    val req = 0xc600
    builder.writeShort(magic)
    builder.writeShort(req)
    builder.writeLong(requestId)
  }
}


