package cn.goldlokedu.alicomp.documents

import java.nio.ByteOrder

import akka.util.{ByteString, ByteStringBuilder}

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

  implicit def toDubboRequest(obj: BenchmarkRequest): ByteString = {
    val builder = ByteString.newBuilder

    createDubboRequestHeader(builder, obj.requestId)
    val body = createDubboRequestBody(obj)

    val size = body.size
    builder.putInt(size)
    builder.result() ++ body
  }

  @inline
  private def createDubboRequestBody(obj: BenchmarkRequest): ByteString = {
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
  private def createDubboRequestHeader(builder: ByteStringBuilder, requestId: Long) = {
    // magic
    val magic = 0xdabb
    //req + 2way + event + serialization id + status
    val req = 0xc600
    builder.putShort(magic)
    builder.putShort(req)
    builder.putLong(requestId)
  }
}


