package cn.goldlokedu.alicomp.consumer.netty

import java.util.concurrent.TimeUnit

import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessage}
import io.netty.buffer.ByteBuf
import io.netty.channel.{Channel, ChannelHandlerContext, ChannelInboundHandlerAdapter}
import io.netty.util.ReferenceCountUtil

import scala.collection.mutable

class ProviderAgentHandler extends ChannelInboundHandlerAdapter {
  val workingRequests: mutable.LongMap[(Channel, Long)] = mutable.LongMap()
  var total = 0
  var latencyAverge = 0.0

  override def userEventTriggered(ctx: ChannelHandlerContext, evt: scala.Any): Unit = {
    evt match {
      case req: BenchmarkRequest =>
        workingRequests(req.requestId) = req.replyTo -> System.currentTimeMillis()
        ctx.writeAndFlush(req.byteBuf, ctx.voidPromise())
      case _ =>
    }
  }

  override def channelActive(ctx: ChannelHandlerContext): Unit = {
    ctx.channel().eventLoop().scheduleAtFixedRate({ () =>
      println(s"avg latency = $latencyAverge ms, total = $total")
    }, 30, 1, TimeUnit.SECONDS)
  }

  override def channelRead(ctx: ChannelHandlerContext, msg: scala.Any): Unit = {
    msg match {
      case buf: ByteBuf =>
        for {
          isEvent <- DubboMessage.extractIsEvent(buf) if !isEvent
          requestId <- DubboMessage.extractRequestId(buf)
        } {
          workingRequests.remove(requestId) match {
            case Some((channel, start)) =>
              channel.writeAndFlush(BenchmarkResponse.toHttpResponse(buf), channel.voidPromise())
              val end = System.currentTimeMillis()
              val dif = end - start

              latencyAverge = (total * latencyAverge + dif) / (total + 1)
              total = total + 1
            case None =>
          }
        }
      case _ =>
    }
    ReferenceCountUtil.release(msg)
  }

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush()
  }
}
