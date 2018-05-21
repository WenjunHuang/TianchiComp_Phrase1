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
  var waitingRequests: Seq[(ActorRef, BenchmarkRequest)] = Nil
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

//      implicit val ec = context.dispatcher
//      context.system.scheduler.schedule(1 second, 1 second, self, Print)
  }

  def ready: Receive = {
    case Print =>
      log.info(s"isWriting: ${isWriting},waiting: ${waitingRequests.size}, working: ${workingRequests.size}")
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
      sendPendingRequests()
    case msg: BenchmarkRequest =>
      waitingRequests = waitingRequests :+ (sender -> msg)
      sendPendingRequests()
    case DoneWrite =>
      isWriting = false
      sendPendingRequests()
  }

  @inline
  def sendPendingRequests() = {
    (isWriting, waitingRequests.isEmpty) match {
      case (true, _) =>
      case (_, true) =>
      case (false, false) =>
        val toSend = waitingRequests.foldLeft(ByteString.empty) { (accum, msg) =>
          workingRequests(msg._2.requestId) = msg._1
          accum ++ msg._2
        }
        if (toSend.nonEmpty) {
          connection.get ! Write(toSend, DoneWrite)
          isWriting = true
          waitingRequests = Nil
        }
    }
  }
}

object ProviderAgentClientActor {

  case object DoneWrite extends Event

  case object Print

}
