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

  @inline
  implicit def toDubboRequest(obj: BenchmarkRequest): ByteString = {
    val builder = ByteString.newBuilder

    createDubboRequestHeader(builder, obj.requestId)
    val body = createDubboRequestBody(obj.interface, obj.method, obj.parameterTypeString, obj.parameter)

    val size = body.size
    builder.putInt(size)
    builder.append(body)
    builder.result()
  }

  def makeDubboRequest(requestId: Long,
                       interface: String,
                       method: String,
                       parameterTypeString: String,
                       parameter: String): ByteString = {
    val builder = ByteString.newBuilder
    createDubboRequestHeader(builder, requestId)
    val body = createDubboRequestBody(interface, method, parameterTypeString, parameter)

    builder.putInt(body.size)
    builder.append(body)
    builder.result()
  }

  @inline
  private def createDubboRequestBody(interface: String, method: String, parameterTypeString: String, parameter: String): ByteString = {
    val bodyBuilder = ByteString.newBuilder
    val body = Seq(DubboVersion, // dubbo version
      s""""${interface}"""", // service name
      RequestVersion, // service version
      s""""${method}"""", // method name
      s""""${parameterTypeString}"""", // method parameter type
      s""""${parameter}"""", // method arguments
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


