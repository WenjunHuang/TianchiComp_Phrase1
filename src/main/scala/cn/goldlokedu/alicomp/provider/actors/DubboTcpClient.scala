package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{Event, Received, Write}
import akka.routing.Router
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.{DubboMessage, DubboMessageBuilder}

class DubboTcpClient(connection: ActorRef,
                     dubboActor: Router) extends Actor with ActorLogging {

  import DubboTcpClient._

  var dubboMessageHandler = DubboMessageBuilder(ByteString.empty)
  var isWriting = false
  var pendingResults: Seq[ByteString] = Nil

  override def receive: Receive = {
    case Received(data) =>
      val it = dubboMessageHandler.feedRaw(data)
      dubboMessageHandler = it._1
      if (it._2.nonEmpty)
        dubboActor.route(it._2, self)
    case DoneWrite =>
      isWriting = false
      trySendBackPendingResults()
    case msgs: Seq[ByteString] =>
      // dubbo 结果
      pendingResults ++= msgs
      trySendBackPendingResults()
    case Print =>
      log.info(s"pending: ${pendingResults.size}")
  }

  def trySendBackPendingResults() = {
    (isWriting, pendingResults.size >= 10) match {
      case (false, true) =>
        val toSend = pendingResults.foldLeft(ByteString.empty) { (accum, msg) =>
          accum ++ msg
        }
        connection ! Write(toSend, DoneWrite)
        pendingResults = Nil
      case _=>
    }
  }
}

object DubboTcpClient {

  case object DoneWrite extends Event

  case object Print

}
