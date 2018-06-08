package cn.goldlokedu.alicomp.consumer.netty

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel

class ProviderAgentClientInitializer extends ChannelInitializer[SocketChannel] {
  override def initChannel(ch: SocketChannel): Unit = {

  }
}
