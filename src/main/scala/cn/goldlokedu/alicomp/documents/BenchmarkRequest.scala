package cn.goldlokedu.alicomp.documents

import java.nio.ByteOrder

import io.netty.buffer.{ByteBuf, ByteBufAllocator}

case class BenchmarkRequest(requestId: Long,
                            interface: String,
                            method: String,
                            parameterTypeString: String,
                            parameter: String)

object BenchmarkRequest {
  implicit val bo: ByteOrder = ByteOrder.BIG_ENDIAN

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
      s""""${interface}"""", // service name
      RequestVersion, // service version
      s""""${method}"""", // method name
      s""""${parameterTypeString}"""", // method parameter type
      s""""${parameter}"""", // method arguments
      s"{}"
    ).mkString("\n").getBytes("UTF-8")
    byteBuf.writeInt(body.size)
    byteBuf.writeBytes(body)
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


