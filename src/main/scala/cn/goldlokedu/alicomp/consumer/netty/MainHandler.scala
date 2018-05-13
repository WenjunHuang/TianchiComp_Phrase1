package cn.goldlokedu.alicomp.consumer.netty

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.{FullHttpRequest, HttpMethod}

import scala.concurrent.ExecutionContext

class MainHandler(implicit executor:ExecutionContext) extends SimpleChannelInboundHandler[FullHttpRequest] {
  override def channelRead0(ctx: ChannelHandlerContext, msg: FullHttpRequest): Unit = {
    msg.method() match {
      case HttpMethod.POST => {
      }
    }
  }
}
