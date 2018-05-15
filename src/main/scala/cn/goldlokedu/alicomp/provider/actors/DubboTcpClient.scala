package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.{Event, Received, Write}
import akka.routing.Router
import akka.util.ByteString
import cn.goldlokedu.alicomp.documents.{DubboMessage, DubboMessageBuilder}
import cn.goldlokedu.alicomp.provider.actors.DubboTcpClient.DoneWrite

import scala.collection.mutable

class DubboTcpClient(connection: ActorRef,
                     dubboActor: Router) extends Actor {
  import DubboTcpClient._

  var dubboMessageHandler = DubboMessageBuilder(ByteString.empty)
  var isWriting = false
  var pendingResults:mutable.Queue[DubboMessage] = mutable.Queue.empty

  override def receive: Receive = {
    case Received(data) =>
      val it = dubboMessageHandler.feed(data)
      dubboMessageHandler = it._1

      if (it._2.nonEmpty)
        dubboActor.route(it._2, self)
    case msgs:DubboMessage =>
      // dubbo 结果
      pendingResults.enqueue(msgs)
      if (!isWriting) {
        sendBack()
      }
    case DoneWrite =>
      isWriting = false
  }

  def sendBack() = {
    val msgs = pendingResults.dequeueAll(_=>true)
    val builder = ByteString.newBuilder
    msgs.foreach{msg =>
      builder.append(msg.toByteString)
    }

    isWriting = true
    connection ! Write(builder.result,DoneWrite)
  }
}
object DubboTcpClient{
  case object DoneWrite extends Event
}
