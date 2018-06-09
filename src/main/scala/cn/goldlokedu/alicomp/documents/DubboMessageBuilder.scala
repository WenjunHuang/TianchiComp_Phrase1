package cn.goldlokedu.alicomp.documents

import java.lang.{Byte => JByte}

import io.netty.buffer.{ByteBuf, ByteBufAllocator}
import io.netty.util.ReferenceCountUtil

import scala.annotation.tailrec

/**
  * 根据字节流构建出正确的Dubbo消息
  */
case class DubboMessageBuilder(first: ByteBuf, alloc: ByteBufAllocator) {
  def feedRaw(next: ByteBuf): (DubboMessageBuilder, Seq[ByteBuf]) = {
    val composit = alloc.compositeBuffer()
    extractRaw(composit.addComponents(true,first, next))
  }

  private def extractRaw(data: ByteBuf): (DubboMessageBuilder, Seq[ByteBuf]) = {
    @tailrec
    def fold(restData: ByteBuf, messages: Seq[ByteBuf]): (ByteBuf, Seq[ByteBuf]) = {
      val restSize = restData.readableBytes()
      if (restSize > 16) {
        val dataLength = JByte.toUnsignedInt(restData.getByte(12)) << 24 |
          JByte.toUnsignedInt(restData.getByte(13)) << 16 |
          JByte.toUnsignedInt(restData.getByte(14)) << 8 |
          JByte.toUnsignedInt(restData.getByte(15))

        // 消息已经完整，开始解析
        val total = 16 + dataLength
        if (restSize >= total) {
          // 内容数据已经有了
          val split = restData.retainedSlice(restData.readerIndex(), total)
          val rest = restData.retainedSlice(total, restSize - total)
          ReferenceCountUtil.release(restData)

          fold(rest, split +: messages)
        } else {
          (restData, messages) // 只有头部，没有body
        }
      } else {
        (restData, messages) // 头部不完整
      }
    }

    val r = fold(data, Nil)
    (DubboMessageBuilder(r._1, alloc), r._2.reverse)
  }
}
