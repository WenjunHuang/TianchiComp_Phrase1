package cn.goldlokedu.alicomp.provider.netty

import cn.goldlokedu.alicomp.documents.DubboMessage
import io.netty.buffer.{ByteBuf, Unpooled}
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

class DubboToProtocalHandler extends SimpleChannelInboundHandler[ByteBuf] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf): Unit = {
    for {
      isResponse <- DubboMessage.extractIsResponse(msg) if isResponse
      reqId <- DubboMessage.extractRequestId(msg)
      status <- DubboMessage.extractStatus(msg)
    } {
      val start = msg.readerIndex() + DubboMessage.HeaderWithLength + 2
      val length = msg.readableBytes() - (DubboMessage.HeaderWithLength + 2 + 1)

      val slice = msg.slice(start,length)
      val result = if (status == 20) {
        if (slice.getByte(0) == 45) {
          // 负数
          slice.readByte()
          var accum = 0
          slice.forEachByte((b: Byte) => {
            accum = accum * 10 + (b - 48) // 数字的ascii码-48=数字值
            true
          })
          -accum
        } else {
          // 正数
          var accum = 0
          slice.forEachByte((b: Byte) => {
            accum = accum * 10 + (b - 48) // 数字的ascii码-48=数字值
            true
          })
          accum
        }
      } else
        0

      val buf = msg.retain()
      buf.resetReaderIndex()
      buf.resetWriterIndex()
      buf.writeLong(reqId)
      buf.writeShort(status.toInt)
      buf.writeInt(result)
      ctx.fireChannelRead(buf)
    }
  }
}
