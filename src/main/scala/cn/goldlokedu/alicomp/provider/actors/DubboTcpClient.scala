package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingAdapter
import akka.io.Tcp.{Event, Received, Write}
import akka.util.ByteString

class DubboTcpClient(connection: ActorRef,
                     dubboHost:String,
                     dubboPort:Int)(implicit log:LoggingAdapter) extends Actor with ActorLogging {
  import DubboTcpClient._

  var isWriting = false
  var pendingResults: ByteString = ByteString.empty
  var dubboActor:ActorRef = _

  override def preStart(): Unit = {
    dubboActor = context
      .actorOf(Props(new DubboActor(dubboHost, dubboPort)), s"dubbo")
    dubboActor ! "ReplyTo"
  }

  override def receive: Receive = {
    case Received(data) =>
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
