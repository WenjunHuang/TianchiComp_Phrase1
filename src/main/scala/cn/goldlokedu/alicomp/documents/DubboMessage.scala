package cn.goldlokedu.alicomp.documents

import io.netty.buffer.ByteBuf

object DubboMessage {
  val HeaderSize = 12
  val HeaderWithLength = 16
  def extractRequestId(msg: ByteBuf): Option[Long] = {
    if (msg.readableBytes() < HeaderSize)
      None
    else {
      val requestId = msg.getLong(4)
      Some(requestId)
    }
  }

  def extractStatus(msg: ByteBuf): Option[Byte] = {
    if (msg.readableBytes() < HeaderSize)
      None
    else {
      val status = msg.getByte(3)
      Some(status)
    }
  }

  def extractIsEvent(msg:ByteBuf):Option[Boolean] = {
    if (msg.readableBytes() < HeaderSize)
      None
    else {
      val b = msg.getByte(2)
      if ((b & 0x20) != 0)
        Some(true)
      else
        Some(false)
    }
  }

  def extractIsResponse(msg: ByteBuf): Option[Boolean] = {
    if (msg.readableBytes() < HeaderSize)
      None
    else {
      val b = msg.getByte(2)
      if ((b & 0x80) == 0 && (b & 0x20) == 0)
        Some(true)
      else
        Some(false)
    }
  }
}
