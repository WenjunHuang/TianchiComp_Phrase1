package cn.goldlokedu.alicomp.documents

import java.nio.ByteOrder

import akka.util.{ByteString, ByteStringBuilder}
import cn.goldlokedu.alicomp.documents.BenchmarkRequest.{DubboVersion, RequestVersion}
import io.netty.buffer.{ByteBuf, ByteBufAllocator}

case class DubboMessage(isRequest: Boolean,
                        is2Way: Boolean,
                        isEvent: Boolean,
                        serializationId: Byte,
                        status: Byte,
                        requestId: Long,
                        dataLength: Int,
                        body: ByteString) {
  def isResponse: Boolean = !isRequest && !isEvent

  def toByteString = {
    implicit val bo = ByteOrder.BIG_ENDIAN
    val builder = new ByteStringBuilder()
    // magic
    val magic = 0xdabb
    val fastJsonSerId = 0x600
    //req + 2way + event + serialization id + status

    var head: Short = 0
    if (isRequest)
      head = (head | 0x8000).toShort
    if (is2Way)
      head = (head | 0x4000).toShort
    if (isEvent)
      head = (head | 0x2000).toShort
    head = (head | fastJsonSerId).toShort
    head = (head | status).toShort

    //    val req = 0xc600
    builder.putShort(magic)
    builder.putShort(head)
    builder.putLong(requestId)
    builder.putInt(dataLength)
    builder.append(body)
    builder.result()
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
}

object DubboMessage {
  def extractRequestId(msg: ByteString): Option[Long] = {
    if (msg.size < 12)
      None
    else {
      val requestId = msg.slice(4, 12).zipWithIndex.foldLeft(0L) { (accum, byte) =>
        accum | (java.lang.Byte.toUnsignedLong(byte._1) << ((7 - byte._2) * 8))
      }
      Some(requestId)
    }
  }

  def extractIsResponse(msg: ByteString): Option[Boolean] = {
    if (msg.size < 12)
      None
    else {
      val b = msg(3)
      if ((b & 0x8000) == 0 && (b & 0x20) == 0)
        Some(true)
      else
        Some(false)
    }
  }
}
