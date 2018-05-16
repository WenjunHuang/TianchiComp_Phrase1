package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.{Event, Received, Write}
import akka.routing.Router
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.{DubboMessage, DubboMessageBuilder}

import scala.collection.mutable

class DubboTcpClient(connection: ActorRef,
                     dubboActor: Router) extends Actor {

  import DubboTcpClient._

  var dubboMessageHandler = DubboMessageBuilder(ByteString.empty)
  var isWriting = false
  var pendingResults: mutable.Queue[DubboMessage] = mutable.Queue.empty

  override def receive: Receive = {
    case Received(data) =>
      val it = dubboMessageHandler.feed(data)
      dubboMessageHandler = it._1

      if (it._2.nonEmpty)
        dubboActor.route(it._2, self)
    case DoneWrite =>
      isWriting = false
    case msg: DubboMessage =>
      // dubbo 结果
      pendingResults.enqueue(msg)
      if (!isWriting) {
        sendBack()
      }
  }

  def sendBack() = {
    if (pendingResults.nonEmpty) {
      val toSend = pendingResults.foldLeft(ByteString.empty) { (accum, msg) =>
        accum ++ msg.toByteString
      }
      connection ! Write(toSend, DoneWrite)
      isWriting = true
      pendingResults.clear()
    }
  }
}

object DubboTcpClient {

  case object DoneWrite extends Event

}
