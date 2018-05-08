package cn.goldlokedu.alicomp.provider.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Stash}
import akka.event.LoggingAdapter
import akka.io.Tcp.Event
import akka.io.{IO, Tcp}
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessageBuilder}
import scala.concurrent.duration._

import scala.collection.mutable

class DubboActor(dubboHost: String,
                 dubboPort: Int,
                 threhold: Int)(implicit val logger: LoggingAdapter) extends Actor with Stash {

  import DubboActor._
  import Tcp._
  import context.system

  // Akka IO
  var connection: Option[ActorRef] = None

  var currentWritingRequest: Option[(ActorRef, BenchmarkRequest)] = None

  // 当前正在执行的请求
  val runningRequests: mutable.Map[Long, ActorRef] = mutable.Map.empty

  // 未发送的请求
  val pendingRequests: mutable.Queue[(ActorRef, BenchmarkRequest)] = mutable.Queue.empty

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
      context stop self
    case Connected(remote, local) =>
      logger.info(s"dubbo connected")
      connection = Some(sender())
      connection.get ! Register(self)

      unstashAll()
      // debug
      implicit val ec = context.dispatcher
      context.system.scheduler.schedule(1 second, 1 second, self, PrintPayload)
      context become ready
    case _ =>
      stash()
  }

  def ready: Receive = {
    case PrintPayload =>
      println(s"${self.path.name}: pending: ${pendingRequests.size}, working: ${runningRequests.size}")

    case msg: BenchmarkRequest =>
      trySendRequestToDubbo(sender, msg)
    case DoneWrite =>
      //请求的数据已经完全发送给dubbo了，接着发下一个（如果还有的话）
      currentWritingRequest = None
      trySendNextPending()
    case Received(data) =>
      val (newBuilder, messages) = dubboMessageBuilder.feed(data)
      dubboMessageBuilder = newBuilder

      // 有可能一次读取就获取了多个回复
      messages.foreach { msg =>
        if (msg.isResponse) {
          runningRequests.remove(msg.requestId) match {
            case Some(replyTo) =>
              replyTo ! BenchmarkResponse(msg).get
              trySendNextPending()
            case None =>
              // 收到一个未知requestId的回复?这可能是个bug
              logger.error(s"unknown dubbo response ${msg.requestId}, this message is not send by me")
          }
        }
      }

    case _: ConnectionClosed =>
      logger.info("connection closed by dubbo,try reconnect")
      connectToDubbo()
      context become connectingToDubbo
  }

  private def connectToDubbo() = {
    IO(Tcp) ! Connect(new InetSocketAddress(dubboHost, dubboPort))
  }

  private def trySendNextPending() = {
    (isBelowThrehold, hasAnyPending, notWriting) match {
      case (true, true, true) =>
        pendingRequests.dequeueFirst(_ => true) match {
          case Some((replyTo, request)) =>
            sendRequestToDubbo(replyTo, request)
          case None =>
        }
      case _ =>
    }
  }

  private def trySendRequestToDubbo(replyTo: ActorRef, request: BenchmarkRequest) = {
    (isBelowThrehold, noPending, notWriting) match {
      case (true, true, true) =>
        sendRequestToDubbo(replyTo, request)
      case _ =>
        pendingRequests.enqueue((replyTo, request))
    }
  }

  @inline
  private def notWriting = {
    !isWriting
  }

  @inline
  private def isWriting = {
    currentWritingRequest.nonEmpty
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

  private def sendRequestToDubbo(replyTo: ActorRef, request: BenchmarkRequest) = {
    currentWritingRequest = Some((replyTo, request))
    runningRequests += request.requestId -> replyTo
    connection.get ! Write(request, DoneWrite)
  }

  private def pendRequest(replyTo: ActorRef, request: BenchmarkRequest) = {
    pendingRequests.enqueue((replyTo, request))
  }

}

object DubboActor {

  case object Init

  case object DoneWrite extends Event

  case object PrintPayload

}
