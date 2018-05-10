package cn.goldlokedu.alicomp.consumer.actors

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingAdapter
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, CapacityType}

import scala.concurrent.ExecutionContext
import scala.util.Random

class ConsumerAgentActor(providerAgents: Map[CapacityType.Value, ActorRef])(implicit ec: ExecutionContext,
                                                                            to: Timeout,
                                                                            logger: LoggingAdapter) extends Actor {

  import ConsumerAgentActor._

  override def preStart(): Unit = {
    self ! Init
  }

  override def receive: Receive = {
    case req: BenchmarkRequest =>
      selectProviderAgent.tell(req, sender)
  }

  def selectProviderAgent: ActorRef = {
    val roll = Random.nextInt(6)
    val cap = roll match {
      case 0 =>
        CapacityType.S
      case x if (1 to 2).contains(x) =>
        CapacityType.M
      case x if (3 to 5).contains(x) =>
        CapacityType.L
      case _ =>
        CapacityType.L
    }

    providerAgents(cap)
  }
}

object ConsumerAgentActor {

  case object Init

}
