package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp.{Event, Received, Write}
import akka.util.ByteString

class DubboTcpClient(connection: ActorRef,
                     dubboActor: ActorRef) extends Actor with ActorLogging {

  import DubboTcpClient._

  //  var dubboMessageHandler = DubboMessageBuilder(ByteString.empty)
  var isWriting = false
  var pendingResults: ByteString = ByteString.empty

  override def preStart(): Unit = {
    dubboActor ! "ReplyTo"
  }

  override def receive: Receive = {
    case Received(data) =>
      //      val it = dubboMessageHandler.feedRaw(data)
      //      dubboMessageHandler = it._1
      //      if (it._2.nonEmpty)
      //        dubboActor.route(it._2, self)
      dubboActor ! data
    case DoneWrite =>
      isWriting = false
      trySendBackPendingResults()
    case msgs: ByteString =>
      // dubbo 结果
      pendingResults = pendingResults ++ msgs
      trySendBackPendingResults()
    case Print =>
      log.info(s"pending: ${pendingResults.size}")
  }

  def trySendBackPendingResults() = {
    (isWriting, pendingResults.nonEmpty) match {
      case (false, true) =>
        //        val toSend = pendingResults.foldLeft(ByteString.empty) { (accum, msg) =>
        //          accum ++ msg
        //        }
        connection ! Write(pendingResults, DoneWrite)
        isWriting = true
        pendingResults = ByteString.empty
      case _ =>
    }
  }
}

object DubboTcpClient {

  case object DoneWrite extends Event

  case object Print

}
