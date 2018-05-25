package cn.goldlokedu.alicomp.documents

import akka.util.ByteString

import scala.annotation.tailrec

/**
  * 根据字节流构建出正确的Dubbo消息
  */
case class DubboMessageBuilder(first: ByteString) {
  def feed(next: ByteString): (DubboMessageBuilder, Seq[DubboMessage]) = {
    extract(first ++ next)
  }

  def feedRaw(next: ByteString): (DubboMessageBuilder, Seq[ByteString]) = {
    extractRaw(first ++ next)
  }

  private def extractRaw(data: ByteString): (DubboMessageBuilder, Seq[ByteString]) = {
    @tailrec
    def fold(restData: ByteString, messages: Seq[ByteString]): (ByteString, Seq[ByteString]) = {
      if (restData.size > 16) {
        // 16个字节的头部
//        val header = restData.take(16)
        //        val dataLength = header.slice(12, 16).zipWithIndex.foldLeft(0) { (accum, byte) =>
        //          accum | (java.lang.Byte.toUnsignedInt(byte._1) << ((3 - byte._2) * 8))
        //        }

        val dataLengthBytes = restData.slice(12, 16)
        val dataLength = java.lang.Byte.toUnsignedInt(dataLengthBytes(0)) << 24 |
          java.lang.Byte.toUnsignedInt(dataLengthBytes(1)) << 16 |
          java.lang.Byte.toUnsignedInt(dataLengthBytes(2)) << 8 |
          java.lang.Byte.toUnsignedInt(dataLengthBytes(3))

        // 消息已经完整，开始解析
        if (restData.size >= 16 + dataLength) {

          // 内容数据已经有了
          val split = restData.splitAt(16 + dataLength)
          fold(split._2, split._1 +: messages)
        } else {
          (restData, messages) // 只有头部，没有body
        }
      } else {
        (restData, messages) // 头部不完整
      }
    }

    val r = fold(data, Nil)
    (DubboMessageBuilder(r._1), r._2.reverse)
  }

  private def extract(data: ByteString): (DubboMessageBuilder, Seq[DubboMessage]) = {
    @tailrec
    def fold(restData: ByteString, messages: Seq[DubboMessage]): (ByteString, Seq[DubboMessage]) = {
      if (restData.size > 16) {
        // 16个字节的头部
        val header = restData.take(16)
        val dataLength = header.slice(12, 16).zipWithIndex.foldLeft(0) { (accum, byte) =>
          accum | (java.lang.Byte.toUnsignedInt(byte._1) << ((3 - byte._2) * 8))
        }

        // 消息已经完整，开始解析
        if (restData.size >= 16 + dataLength) {
          val isReq = (header(2) & 0x80) != 0
          val is2Way = (header(2) & 0x40) != 0
          val isEvent = (header(2) & 0x20) != 0
          val serId = (header(2) & 0xF).toByte
          val status = header(3)

          val requestId = header.slice(4, 12).zipWithIndex.foldLeft(0L) { (accum, byte) =>
            accum | (java.lang.Byte.toUnsignedLong(byte._1) << ((7 - byte._2) * 8))
          }

          // 内容数据已经有了
          val split = restData.splitAt(16 + dataLength)
          val body = split._1.drop(16)
          fold(split._2, messages :+ DubboMessage(isReq, is2Way, isEvent, serId, status, requestId, dataLength, body))
        } else {
          (restData, messages) // 只有头部，没有body
        }
      } else {
        (restData, messages) // 头部不完整
      }
    }

    val r = fold(data, Nil)
    (DubboMessageBuilder(r._1), r._2.reverse)
  }
}
