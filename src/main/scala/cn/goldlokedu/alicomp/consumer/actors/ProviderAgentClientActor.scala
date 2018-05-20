package cn.goldlokedu.alicomp.consumer.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessageBuilder}

import scala.collection.immutable.Queue
import scala.collection.mutable


class ProviderAgentClientActor(providerName: String,
                               providerAgentHost: String,
                               providerAgentPort: Int) extends Actor with ActorLogging {

  import ProviderAgentClientActor._
  import context.system

  val BatchCount = 100

  var dubboMessageHandler = DubboMessageBuilder(ByteString.empty)

  var isWriting = false
  var pendingRequests: Seq[(ActorRef, BenchmarkRequest)] = Nil

  val workingRequests: mutable.Map[Long, ActorRef] = mutable.Map.empty
  var connection: Option[ActorRef] = None

  IO(Tcp) ! Connect(new InetSocketAddress(providerAgentHost, providerAgentPort))

  override def receive: Receive = {
    case CommandFailed(_: Connect) =>
      log.error(s"can not connect to provider: $providerName at $providerAgentHost:$providerAgentPort")
    case Connected(remote, local) =>
      log.info(
        s"""
           |provider: $providerName connected
           |host: ${remote.getHostString}, port: ${remote.getPort}
           |my_host: ${local.getHostString},my_port: ${local.getPort}""".stripMargin)
      connection = Some(sender())
      connection.get ! Register(self)
      context become ready
  }

  def ready: Receive = {
    case Received(data) =>
      val (newHandler, msgs) = dubboMessageHandler.feed(data)
      dubboMessageHandler = newHandler
      if (msgs.nonEmpty) {
        msgs.foreach { msg =>
          workingRequests.remove(msg.requestId) match {
            case Some(actorRef) =>
              actorRef ! BenchmarkResponse(msg)
            case _ =>
          }
        }
      }

    case msg: BenchmarkRequest =>
      sendBenchmark(msg)
    case DoneWrite =>
      isWriting = false
      sendPendingRequests()
  }

  @inline
  def sendBenchmark(msg: BenchmarkRequest) = {
    (isWriting, pendingRequests.isEmpty) match {
      case (true, _) =>
        pendingRequests :+= (sender -> msg)
      case (true, true) =>
        pendingRequests :+= (sender -> msg)
      case (false,true) =>
        pendingRequests :+= (sender -> msg)
        sendPendingRequests()
      case _ =>
        send(Seq(sender -> msg))
    }
  }

  @inline
  def sendPendingRequests() = {
    (isWriting, pendingRequests.isEmpty) match {
      case (true, _) =>
      case (_,true) =>
      case (false, false) =>
        send(pendingRequests)
        pendingRequests = Nil
    }
  }

  @inline
  def send(msgs: Seq[(ActorRef, BenchmarkRequest)]) = {
    val toSend = msgs.foldLeft(ByteString.empty) { (accum, msg) =>
      workingRequests(msg._2.requestId) = msg._1
      accum ++ msg._2
    }
    isWriting = true
    connection.get ! Write(toSend, DoneWrite)
  }
}

object ProviderAgentClientActor {

  case object DoneWrite extends Event

}
