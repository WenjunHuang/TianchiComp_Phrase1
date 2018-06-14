package cn.goldlokedu.alicomp.documents

import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, ByteBufAllocator, ByteBufUtil, Unpooled}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpResponse, HttpResponseStatus, HttpVersion}
import io.netty.util.{AsciiString, ReferenceCountUtil}

case class BenchmarkResponse(requestId: Long,
                             status: Int,
                             result: Option[Int])

object BenchmarkResponse {
  val CONTENT_TYPE = AsciiString.cached("Content-Type")
  val CONTENT_LENGTH = AsciiString.cached("Content-Length")
  val CONNECTION = AsciiString.cached("Connection")
  val KEEP_ALIVE = AsciiString.cached("keep-alive")
  val TEXT_PLAIN = AsciiString.cached("text/plain")

  def toHttpResponse(message: ByteBuf): FullHttpResponse = {
    // 测试的结果是个32位整数值，但是dubbo用fastjson编码后得出的是一个字符串，例如 9900，结果是"1\n9900\n"字符串
    val status = DubboMessage.extractStatus(message)
//    message.skipBytes(2)
//    val status = message.readByte()
    // 头两个字节是"1\n",最后一个字节是"\n",todo 如果是windows，那么是\n\r
    message.readerIndex(message.readerIndex() + DubboMessage.HeaderWithLength + 2)
    message.writerIndex(message.writerIndex() - 1)
    val result = if (status.get == 20) {
      if (message.getByte(message.readerIndex()) == 45) {
        // 负数
        message.readByte()
        var accum = 0
        message.forEachByte((b: Byte) => {
          accum = accum * 10 + (b - 48) // 数字的ascii码-48=数字值
          true
        })
        Some(-accum)
      } else {
        // 正数
        var accum = 0
        message.forEachByte((b: Byte) => {
          accum = accum * 10 + (b - 48) // 数字的ascii码-48=数字值
          true
        })
        Some(accum)
      }
    } else
      None

    val httpResponse = result match {
      case None =>
        val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
          HttpResponseStatus.INTERNAL_SERVER_ERROR)
        response.headers().setInt(CONTENT_LENGTH, 0)
        response.headers().set(CONNECTION, KEEP_ALIVE)
        response
      case Some(value) =>
        val buf = Unpooled.copiedBuffer(String.valueOf(value), StandardCharsets.UTF_8)
        val response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK, buf)
        response.headers().set(CONTENT_TYPE, TEXT_PLAIN)
        response.headers().setInt(CONTENT_LENGTH, response.content().readableBytes())
        response.headers().set(CONNECTION, KEEP_ALIVE)

        response
    }

    ReferenceCountUtil.release(message)

    httpResponse
  }
}

