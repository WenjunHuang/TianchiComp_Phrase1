package cn.goldlokedu.alicomp.provider.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingAdapter
import akka.io.Tcp.Event
import akka.io.{IO, Tcp}
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents._

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class DubboActor(dubboHost: String,
                 dubboPort: Int,
                 threhold: Int)(implicit logger: LoggingAdapter, ec: ExecutionContext) extends Actor {

  import DubboActor._
  import Tcp._

  var statistics = 0.0
  var completedCount = 0L
  var largestLatency = 0.0

  // Akka IO
  var connection: Option[ActorRef] = None

  var isWriting = false

  // 当前正在执行的请求
  var runningRequests: mutable.Map[Long, RunningRequest] = mutable.Map.empty
  // 当前正在执行的请求数，这个值主要用来加快IO过程
  var runningRequestsCount = 0

  // 未发送的请求
  var pendingRequests: Seq[PendingRequest] = Nil

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
//      context.system.scheduler.schedule(3 minutes, 1 second, self, PrintPayload)
      //                  context.system.scheduler.schedule(1 second, 100 milliseconds, self, TrySend)

      context become ready
  }

  def ready: Receive = {
    case PrintPayload =>
      logger.info(
        s"""
           |${self.path.name}
           |pending: ${pendingRequests.size}
           |working: $runningRequestsCount
           |statics: $statistics ms
           |largest: $largestLatency ms
         """.stripMargin)
    case TrySend =>
      trySendNextPending()
    case msgs: Seq[ByteString] =>
      trySendRequestToDubbo(sender, msgs)
    case DoneWrite =>
      isWriting = false
      trySendNextPending()
    case Received(data) =>
      val (newBuilder, messages) = dubboMessageBuilder.feedRaw(data)
      dubboMessageBuilder = newBuilder
      if (messages.nonEmpty) {
        runningRequestsCount -= messages.size
        trySendNextPending()

        // 有可能一次读取就获取了多个回复
        messages.groupBy { msg =>
          DubboMessage.extractIsResponse(msg) match {
            case Some(true) => runningRequests.remove(DubboMessage.extractRequestId(msg).get)
            case _ => None
          }
        }.foreach {
          case (Some(request), grouped) =>
//            val cur = System.nanoTime()
//            val dif = (cur - request.beginNano) / 1000
//            largestLatency = Math.max(largestLatency, dif)
//            statistics = (statistics * completedCount + dif) / (completedCount + 1)
            completedCount += 1
            request.sender ! grouped
          case _ =>
        }
      }

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

  private def trySendRequestToDubbo(replyTo: ActorRef, msgs: Seq[ByteString]): Unit = {
    (isBelowThrehold, noPending, notWriting) match {
      case (true, true, true) =>
        val a = awailableCount
        val (d, l) = msgs.splitAt(a)
        sendRequestToDubbo(d.map(PendingRequest(replyTo, _, System.nanoTime())))
        pendingRequests ++= l.map(PendingRequest(replyTo, _, System.nanoTime()))
      case (true, false, true) =>
        pendingRequests ++= msgs.map(PendingRequest(replyTo, _, System.nanoTime()))
        val a = awailableCount
        val (d, l) = pendingRequests.splitAt(a)
        sendRequestToDubbo(d)
        pendingRequests = l
      case _ =>
        pendingRequests ++= msgs.map(PendingRequest(replyTo, _, System.nanoTime()))
    }
  }

  @inline
  private def awailableCount = {
    threhold - runningRequestsCount
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
    runningRequestsCount < threhold
  }

  private def sendRequestToDubbo(msgs: Seq[PendingRequest]) = {
    val toSend = msgs.foldLeft(ByteString.empty) { (accum, msg) =>
      runningRequests += DubboMessage.extractRequestId(msg.msg).get -> RunningRequest(msg.sender, msg.beginNano)
      runningRequestsCount += 1
      accum ++ msg.msg
    }
    if (toSend.nonEmpty) {
      connection.get ! Write(toSend, DoneWrite)
      isWriting = true
    }
  }

  private def pendRequest(replyTo: ActorRef, msg: ByteString) = {
    pendingRequests = PendingRequest(replyTo, msg, System.nanoTime()) +: pendingRequests
  }

}

object DubboActor {

  case object Init

  case object DoneWrite extends Event

  case object PrintPayload

  case object TrySend

  case class FeedNewReceived(data: ByteString)

  case class PendingRequest(sender: ActorRef, msg: ByteString, beginNano: Long)

  case class RunningRequest(sender: ActorRef, beginNano: Long)

}
