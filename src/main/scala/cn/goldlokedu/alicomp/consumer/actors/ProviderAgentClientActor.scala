package cn.goldlokedu.alicomp.consumer.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.io.Tcp._
import akka.io.{IO, Tcp}
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, DubboMessageBuilder}

import scala.collection.mutable


class ProviderAgentClientActor(providerName: String,
                               providerAgentHost: String,
                               providerAgentPort: Int) extends Actor with ActorLogging {

  import ProviderAgentClientActor._
  import context.system

  var dubboMessageHandler = DubboMessageBuilder(ByteString.empty)

  var isWriting = false
  val pendingRequests: mutable.Queue[(ActorRef, BenchmarkRequest)] = mutable.Queue.empty
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
      val it = dubboMessageHandler.feed(data)
      dubboMessageHandler = it._1
      it._2.foreach { msg =>
        workingRequests.remove(msg.requestId) match {
          case Some(actorRef) =>
            actorRef ! BenchmarkResponse(msg)
          case _ =>
        }
      }

    case msg: BenchmarkRequest =>
      // dubbo 结果
      pendingRequests.enqueue(sender -> msg)
      if (!isWriting) {
        sendPendingRequests()
      }
    case DoneWrite =>
      isWriting = false
      sendPendingRequests()
  }

  def sendPendingRequests() = {
    val msgs = pendingRequests.dequeueAll(_ => true)
    val builder = ByteString.newBuilder
    msgs.foreach { msg =>
      builder.append(msg._2)
      workingRequests(msg._2.requestId) = msg._1
    }

    isWriting = true
    connection.get ! Write(builder.result, DoneWrite)
  }
}

object ProviderAgentClientActor {

  case object DoneWrite extends Event

}
