package cn.goldlokedu.alicomp.documents

import java.nio.charset.StandardCharsets

import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.handler.codec.http.{DefaultFullHttpResponse, FullHttpResponse, HttpResponseStatus, HttpVersion}
import io.netty.util.ReferenceCountUtil

case class BenchmarkResponse(requestId: Long,
                             status: Int,
                             result: Option[Int])

object BenchmarkResponse {
  def apply(message: ByteBuf): FullHttpResponse = {
    // 测试的结果是个32位整数值，但是dubbo用fastjson编码后得出的是一个字符串，例如 9900，结果是"1\n9900\n"字符串
    val status = DubboMessage.extractStatus(message)
    // 头两个字节是"1\n",最后一个字节是"\n",todo 如果是windows，那么是\n\r
    message.readerIndex(message.readerIndex() + DubboMessage.HeaderSize + 2)
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

    val response = result match {
      case None =>
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR)
      case Some(value) =>
        new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
          HttpResponseStatus.OK,
          Unpooled.copiedBuffer(String.valueOf(value), StandardCharsets.UTF_8))
    }

    ReferenceCountUtil.release(message)
    response
  }
}

