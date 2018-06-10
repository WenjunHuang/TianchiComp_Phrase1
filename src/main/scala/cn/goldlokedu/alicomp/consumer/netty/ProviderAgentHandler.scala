package cn.goldlokedu.alicomp.consumer.netty

import java.util.concurrent.TimeUnit

import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessage}
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable

class ProviderAgentHandler extends ChannelInboundHandlerAdapter {
  val workingRequests: mutable.LongMap[Channel] = mutable.LongMap()

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
    evt match {
      case req: BenchmarkRequest =>
        workingRequests(req.requestId) = req.replyTo
        ctx.writeAndFlush(req.byteBuf, ctx.voidPromise())
      case _ =>
    }
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        for {
          requestId <- DubboMessage.extractRequestId(buf)
        } {
          workingRequests.remove(requestId) match {
            case Some(channel) =>
              channel.writeAndFlush(BenchmarkResponse.toHttpResponse(buf), channel.voidPromise())
            case None =>
          }
        }
      case any =>
    }
    ReferenceCountUtil.release(msg)
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
//    ctx.channel().eventLoop().scheduleAtFixedRate({()=>
//      println(s"working ${workingRequests.size}")
//    },1,1,TimeUnit.SECONDS)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }
}
