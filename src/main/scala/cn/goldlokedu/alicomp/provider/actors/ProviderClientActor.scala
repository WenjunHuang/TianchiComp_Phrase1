package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, ActorRef}
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.DubboMessageBuilder

class ProviderClientActor(dubboRouter: ActorRef) extends Actor {

  import akka.io.Tcp._

  var messageBuilder = DubboMessageBuilder(ByteString.empty)

  override def receive: Receive = {
    case Received(data) =>
      val r = messageBuilder.feed(data)
      messageBuilder = r._1
      r._2.foreach { msg =>
        dubboRouter ! msg
      }
    case PeerClosed => context stop self
  }
}
