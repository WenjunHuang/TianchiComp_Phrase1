package cn.goldlokedu.alicomp.documents

import java.nio.ByteOrder
import java.util.UUID

import akka.util.{ByteString, ByteStringBuilder}

case class DubboMethod(interface: String,
                       method: String,
                       parameterTypeString: String,
                       parameter: String)

object DubboMethod {
  // fastjson 的字符串需要带上""
  val DubboVersion = "\"2.6.0\""
  val RequestVersion = "\"0.0.0\""

  implicit def toDubboRequest(obj: DubboMethod): (ByteString, Long) = {
    val builder = ByteString.newBuilder
    val requestId = UUID.randomUUID().getLeastSignificantBits

    createDubboRequestHeader(builder, requestId)
    val body = createDubboRequestBody(obj)

    val size = body.size
    builder.putInt(size)(ByteOrder.BIG_ENDIAN)
    (builder.result() ++ body, requestId)
  }

  @inline
  def createDubboRequestBody(obj: DubboMethod): ByteString = {
    val bodyBuilder = ByteString.newBuilder
    val body = Seq(DubboVersion, // dubbo version
      s""""${obj.interface}"""", // service name
      RequestVersion, // service version
      s""""${obj.method}"""", // method name
      s""""${obj.parameterTypeString}"""", // method parameter type
      s""""${obj.parameter}"""", // method arguments
      s"{}"
    ).mkString("\n").getBytes("UTF-8")
    bodyBuilder.putBytes(body).result()
  }

  @inline
  def createDubboRequestHeader(builder: ByteStringBuilder, requestId: Long) = {
    // magic + req + 2way + event + serialization id + status
    val magic = 0xdabb
    val req = 0xc600
    //    builder.putShort(magic)
    //    builder.putShort(req)
    builder.putByte(0xda.toByte)
    builder.putByte(0xbb.toByte)
    builder.putByte(0xc6.toByte)
    builder.putByte(0x00.toByte)
    builder.putLong(requestId)(ByteOrder.BIG_ENDIAN)
  }
}


