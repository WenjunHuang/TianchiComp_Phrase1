package cn.goldlokedu.alicomp.consumer.actors

import akka.actor.{Actor, ActorRef}
import akka.event.LoggingAdapter
import akka.pattern._
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class ConsumerAgentActor(implicit etcdClient: EtcdClient,
                         ec: ExecutionContext,
                         to: Timeout,
                         logger:LoggingAdapter) extends Actor {

  import ConsumerAgentActor._

  val providerAgents: mutable.Map[CapacityType.Value, ActorRef] = mutable.Map.empty

  override def preStart(): Unit = {
    self ! Init
  }

  override def receive: Receive = init

  def init: Receive = {
    case Init =>
      etcdClient.providers() pipeTo self
    case pas: Seq[RegisteredAgent] =>
      Future.traverse(
        pas.map { agent =>
          context.actorSelection(agent.address).resolveOne()
            .map { ref =>
              (agent.cap, ref)
            }
        })(identity) pipeTo self
      context become resolvedProviderAgentActorRef
  }

  def resolvedProviderAgentActorRef: Receive = {
    case r: Seq[(CapacityType.Value, ActorRef)] =>
      providerAgents ++= r
      context become ready
      logger.info(s"get provide agens: ${providerAgents.mkString(" ")}")
  }

  def ready: Receive = {
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
