package cn.goldlokedu.alicomp.provider.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.io.{IO, Tcp}

class ProviderServerActor(serverHost: String, serverPort: Int) extends Actor {

  import Tcp._

  implicit val system = context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress(serverHost, serverPort))

  override def receive: Receive = {
    case b@Bound(localAddress) =>
      context.parent ! b
    case CommandFailed(_: Bind) => context stop self
    case c@Connected(remote, local) =>
  }
}
