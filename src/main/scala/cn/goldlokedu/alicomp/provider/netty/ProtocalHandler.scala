package cn.goldlokedu.alicomp.provider.netty

import cn.goldlokedu.alicomp.documents.BenchmarkRequest
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.util.CharsetUtil

import scala.collection.mutable

class ProtocalHandler extends SimpleChannelInboundHandler[ByteBuf] {
  override def channelRead0(ctx: ChannelHandlerContext, req: ByteBuf): Unit = {
    val reqId = req.readLong()
    val size = req.readInt()


    var resolveName = true
    var nameStart = 0
    var nameEnd = 0
    var valueStart = 0
    var valueEnd = 0
    var index = 0
    var newName: ByteBuf = null
    val params = mutable.Map[String, ByteBuf]()
    val slice = req.slice()

    slice.forEachByte((value: Byte) => {
      index += 1
      if (resolveName) {
        if (value == '='.toByte) {
          valueStart = index
          valueEnd = valueStart

          newName = slice.slice(nameStart, nameEnd - nameStart)
          resolveName = false
        } else {
          nameEnd += 1
        }
      } else {
        if (value == '&'.toByte) {
          nameStart = index
          nameEnd = nameStart
          val newValue = slice.slice(valueStart, valueEnd - valueStart)
          val name = newName.toString(CharsetUtil.UTF_8)
          params(name) = newValue
          resolveName = true
        } else {
          valueEnd += 1
        }
      }

      true
    })
    params(newName.toString(CharsetUtil.UTF_8)) = slice.slice(valueStart, valueEnd - valueStart)
    val buffer = BenchmarkRequest.makeDubboRequest(reqId, params.get("parameter").get.retain(), ctx.alloc())
    ctx.fireChannelRead(buffer)
  }
}
