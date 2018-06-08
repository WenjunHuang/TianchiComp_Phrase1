package cn.goldlokedu.alicomp.provider.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.event.LoggingAdapter
import akka.io.Tcp.Event
import akka.io.{IO, Tcp}
import akka.util.ByteString

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class DubboActor(dubboHost: String,
                 dubboPort: Int) extends Actor with ActorLogging {

  import DubboActor._
  import Tcp._

  // Akka IO
  var connection: Option[ActorRef] = None

  var isWriting = false

  // 未发送的请求
  var pendingRequests: ByteString = ByteString.empty

  var replyTo: ActorRef = context.parent

  override def preStart(): Unit = {
    self ! Init
  }

  override def receive: Receive = connectingToDubbo

  def connectingToDubbo: Receive = {
    case Init =>
      connectToDubbo()
    case CommandFailed(_: Connect) =>
      implicit val ec = context.dispatcher
      log.error(s"can not connect to dubbo at $dubboHost:$dubboPort")
      context.system.scheduler.scheduleOnce(5 seconds, self, Init)
    case Connected(remote, local) =>
      log.info(
        s"""
           |dubbo connected
           |host: ${remote.getHostString}, port: ${remote.getPort}
           |my_host: ${local.getHostString},my_port: ${local.getPort}""".stripMargin)
      connection = Some(sender())
      connection.get ! Register(self)
      context become ready
  }

  def ready: Receive = {
    case TrySend =>
      trySendNextPending()
    case msgs: ByteString =>
      trySendRequestToDubbo(sender, msgs)
    case DoneWrite =>
      isWriting = false
      trySendNextPending()
    case Received(data) =>
      replyTo ! data
    case _: ConnectionClosed =>
      log.info("connection closed by dubbo,try reconnect")
      connectToDubbo()
      context become connectingToDubbo
  }

  private def connectToDubbo(): Unit = {
    import context.system
    IO(Tcp) ! Connect(new InetSocketAddress(dubboHost, dubboPort))
  }

  private def trySendNextPending(): Unit = {
    (hasAnyPending, notWriting) match {
      case (true, true) =>
        sendRequestToDubbo(pendingRequests)
        pendingRequests = ByteString.empty
      case _ =>
    }
  }

  private def trySendRequestToDubbo(replyTo: ActorRef, msg: ByteString): Unit = {
    (noPending, notWriting) match {
      case (true, true) =>
        sendRequestToDubbo(msg)
      case (false, true) =>
        pendingRequests ++= msg
        sendRequestToDubbo(pendingRequests)
        pendingRequests = ByteString.empty
      case _ =>
        pendingRequests ++= msg
    }
  }

  @inline
  private def notWriting = {
    !isWriting
  }

  @inline
  private def hasAnyPending = {
    pendingRequests.nonEmpty
  }

  @inline
  private def noPending = {
    !hasAnyPending
  }

  private def sendRequestToDubbo(msgs: ByteString) = {
    if (msgs.nonEmpty) {
      connection.get ! Write(msgs, DoneWrite)
      isWriting = true
    }
  }
}

object DubboActor {

  case object Init

  case object DoneWrite extends Event

  case object TrySend

}
