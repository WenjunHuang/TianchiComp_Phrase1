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
  var pendingResults: Seq[DubboMessage] = Nil

  override def receive: Receive = {
    case Received(data) =>
      val it = dubboMessageHandler.feed(data)
      dubboMessageHandler = it._1

      if (it._2.nonEmpty)
        dubboActor.route(it._2, self)
    case DoneWrite =>
      isWriting = false
      trySendBackPendingResults()
    case msgs: Seq[DubboMessage] =>
      // dubbo 结果
      (isWriting, pendingResults.isEmpty) match {
        case (false, true) =>
          sendBack(msgs)
        case (false, false) =>
          pendingResults ++= msgs
          trySendBackPendingResults()
        case (true, _) =>
          pendingResults ++= msgs
      }
  }

  def trySendBackPendingResults() = {
    sendBack(pendingResults)
    isWriting = true
    pendingResults = Nil
  }

  def sendBack(msgs: Seq[DubboMessage]) = {
    if (msgs.nonEmpty) {
      val toSend = msgs.foldLeft(ByteString.empty) { (accum, msg) =>
        accum ++ msg.toByteString
      }
      connection ! Write(toSend, DoneWrite)
    }
  }
}

object DubboTcpClient {

  case object DoneWrite extends Event

}
