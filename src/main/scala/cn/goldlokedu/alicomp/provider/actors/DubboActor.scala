package cn.goldlokedu.alicomp.provider.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingAdapter
import akka.io.Tcp.Event
import akka.io.{IO, Tcp}
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents._

import scala.collection.immutable.Queue
import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class DubboActor(dubboHost: String,
                 dubboPort: Int,
                 threhold: Int)(implicit logger: LoggingAdapter, ec: ExecutionContext) extends Actor {

  import DubboActor._
  import Tcp._

  // Akka IO
  var connection: Option[ActorRef] = None

  var isWriting = false

  // 当前正在执行的请求
  var runningRequests: mutable.Map[Long, ActorRef] = mutable.Map.empty

  // 未发送的请求
  var pendingRequests: Queue[(ActorRef, DubboMessage)] = Queue.empty

  var dubboMessageBuilder = DubboMessageBuilder(ByteString.empty)

  override def preStart(): Unit = {
    self ! Init
  }

  override def receive: Receive = connectingToDubbo

  def connectingToDubbo: Receive = {
    case Init =>
      connectToDubbo()
    case CommandFailed(_: Connect) =>
      logger.error(s"can not connect to dubbo at $dubboHost:$dubboPort")
      context.system.scheduler.scheduleOnce(5 seconds, self, Init)
    case Connected(remote, local) =>
      logger.info(
        s"""
           |dubbo connected
           |host: ${remote.getHostString}, port: ${remote.getPort}
           |my_host: ${local.getHostString},my_port: ${local.getPort}""".stripMargin)
      connection = Some(sender())
      connection.get ! Register(self)
      // debug
      //      implicit val ec = context.dispatcher
      //      context.system.scheduler.schedule(1 second, 1 second, self, PrintPayload)

      context become ready
  }

  def ready: Receive = {
    case PrintPayload =>
      logger.info(s"${self.path.name}: pending: ${pendingRequests.size}, working: ${runningRequests.size}")
    case msgs: Seq[DubboMessage] =>
      trySendRequestToDubbo(sender, msgs)
    case DoneWrite =>
      isWriting = false
      trySendNextPending()
    case Received(data) =>
      val (newBuilder, messages) = dubboMessageBuilder.feed(data)
      dubboMessageBuilder = newBuilder

      // 有可能一次读取就获取了多个回复
      messages.foreach { msg =>
        if (msg.isResponse) {
          runningRequests.remove(msg.requestId) match {
            case Some(replyTo) =>
              replyTo ! msg
            case None =>
              // 收到一个未知requestId的回复?这可能是个bug
              logger.error(s"unknown dubbo response ${msg.requestId}, this message is not send by me")
          }
        }
      }
      if (messages.nonEmpty)
        trySendNextPending()

    case _: ConnectionClosed =>
      logger.info("connection closed by dubbo,try reconnect")
      connectToDubbo()
      context become connectingToDubbo
  }

  private def connectToDubbo(): Unit = {
    import context.system
    IO(Tcp) ! Connect(new InetSocketAddress(dubboHost, dubboPort))
  }

  private def trySendNextPending(): Unit = {
    (isBelowThrehold, hasAnyPending, notWriting) match {
      case (true, true, true) =>
        val (send, rest) = pendingRequests.splitAt(awailableCount)
        pendingRequests = rest
        sendRequestToDubbo(send)
      case _ =>
    }
  }

  private def trySendRequestToDubbo(replyTo: ActorRef, msgs: Seq[DubboMessage]): Unit = {
    (isBelowThrehold, noPending, notWriting) match {
      case (true, true, true) =>
        val a = awailableCount
        val (d, l) = msgs.splitAt(a)
        sendRequestToDubbo(d.map(replyTo -> _))
        pendingRequests ++= l.map(replyTo -> _)
      case _ =>
        pendingRequests ++= msgs.map(replyTo -> _)
    }
  }

  @inline
  private def awailableCount = {
    threhold - runningRequests.size
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


  @inline
  private def hasThreholded = {
    !isBelowThrehold
  }

  @inline
  private def isBelowThrehold = {
    runningRequests.size < threhold
  }

  private def sendRequestToDubbo(msgs: Seq[(ActorRef, DubboMessage)]) = {
    val toSend = msgs.foldLeft(ByteString.empty) { (accum, msg) =>
      runningRequests += msg._2.requestId -> msg._1
      accum ++ msg._2.toByteString
    }
    if (toSend.nonEmpty) {
      connection.get ! Write(toSend, DoneWrite)
      isWriting = true
    }
  }

  private def pendRequest(replyTo: ActorRef, msg: DubboMessage) = {
    pendingRequests.enqueue(replyTo -> msg)
  }

}

object DubboActor {

  case object Init

  case object DoneWrite extends Event

  case object PrintPayload

}
