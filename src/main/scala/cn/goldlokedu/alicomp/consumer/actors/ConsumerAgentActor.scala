package cn.goldlokedu.alicomp.consumer.actors

import akka.actor.{Actor, ActorRef, Stash, Status, Terminated}
import akka.event.LoggingAdapter
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random
import akka.pattern._

import scala.concurrent.duration._

class ConsumerAgentActor(implicit ec: ExecutionContext,
                         to: Timeout,
                         logger: LoggingAdapter,
                         etcdClient: EtcdClient) extends Actor with Stash {
  var providerAgents: mutable.Map[CapacityType.Value, ActorRef] = mutable.Map.empty

  import ConsumerAgentActor._

  override def preStart(): Unit = {
    self ! GetProviderAgents
    context.system.scheduler.scheduleOnce(5 seconds, self, GetProviderAgents)
  }

  override def receive: Receive = ready

  def ready: Receive = {
    case req: BenchmarkRequest =>
      selectProviderAgent match {
        case Some(actorRef) =>
          actorRef.tell(req, sender)
        case None =>
          sender ! Status.Failure(new Exception("not provider"))
      }
    case GetProviderAgents =>
      tryGetProviderAgents
    case ProviderTerminated(cap) =>
      providerAgents.remove(cap)
  }

  def tryGetProviderAgents = {
    logger.info("begin try get provider agents")
    etcdClient.providers()
      .flatMap { ras =>
        val rest = ras.filterNot(p => providerAgents.contains(p.cap))
        Future.traverse(rest.map { agent =>
          context.system.actorSelection(agent.address).resolveOne
            .map { ar =>
              (agent.cap, Option(ar))
            }.recover {
            case anyError =>
              logger.error(anyError, s"error when try to connect provider")
              (agent.cap, None)
          }
        })(identity)
      }.mapTo[Seq[(CapacityType.Value, Option[ActorRef])]] pipeTo self
    context become getProviderAgents
  }

  def getProviderAgents: Receive = {
    case ra: Seq[(CapacityType.Value, Option[ActorRef])] =>
      logger.info(s"get new provider agents: $ra")
      ra.foreach { r =>
        r._2 match {
          case Some(actorRef) =>
            providerAgents(r._1) = actorRef
            context.watchWith(actorRef, ProviderTerminated(r._1))
          case None =>
        }
      }
      logger.info(s"total provider agents:$providerAgents")
      context become ready
      unstashAll()
    case Status.Failure(cause) =>
      logger.error(cause, s"error in connect to etcd")
    case _ =>
      stash()
  }

  def selectProviderAgent: Option[ActorRef] = {
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

    providerAgents.get(cap).orElse(providerAgents.headOption.map(_._2))
  }
}

object ConsumerAgentActor {

  case object GetProviderAgents

  case class ProviderTerminated(capacityType: CapacityType.Value)

}
