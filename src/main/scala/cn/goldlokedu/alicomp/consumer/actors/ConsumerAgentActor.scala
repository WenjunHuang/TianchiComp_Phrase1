package cn.goldlokedu.alicomp.consumer.actors

import akka.actor.{Actor, ActorRef, Props, Status}
import akka.event.LoggingAdapter
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, CapacityType}
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random

class ConsumerAgentActor(etcdClient: => EtcdClient)(implicit ec: ExecutionContext,
                                                    to: Timeout,
                                                    logger: LoggingAdapter) extends Actor {
  //with Stash {
  var providerAgents: mutable.Map[CapacityType.Value, ActorRef] = mutable.Map.empty

  import ConsumerAgentActor._

  override def preStart(): Unit = {
    context.system.scheduler.scheduleOnce(5 seconds, self, GetProviderAgents)
  }

  override def receive: Receive = ready

  def ready: Receive = {
    case req@BenchmarkRequest(_, _, _, _, _) =>
      selectProviderAgent match {
        case Some(actorRef) =>
          actorRef forward req
        //          actorRef forward toConsumerRequest(req)
        //          actorRef.tell(req, sender)
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
    val etcd = etcdClient
    etcd.providers()
      .map { ras =>
        val rest = ras.filterNot(p => providerAgents.contains(p.cap))
        rest.foreach { agent =>
          val client = context.actorOf(Props(new ProviderAgentClientActor(agent.agentName, agent.host, agent.port)))
          providerAgents(agent.cap) = client
        }
      }
      .onComplete {
        case _ =>
          etcd.shutdown()
      }
  }

  def getProviderAgents: Receive = {
    case ra: Seq[(CapacityType.Value, Option[ActorRef])] =>
      logger.info(s"get new provider agents: $ra")
      ra.foreach {
        r =>
          r._2 match {
            case Some(actorRef) =>
              providerAgents(r._1) = actorRef
              context.watchWith(actorRef, ProviderTerminated(r._1))
            case None =>
          }
      }
      logger.info(s"total provider agents:$providerAgents")
      context become ready
      etcdClient.shutdown()
    //      unstashAll()
    case Status.Failure(cause) =>
      logger.error(cause, s"error in connect to etcd")
    case _ =>
    //      stash()
  }

  def selectProviderAgent: Option[ActorRef] = {
    val roll = Random.nextInt(18)
    val cap = roll match {
      case x if (0 to 3).contains(x) =>
        CapacityType.S
      case x if (4 to 9).contains(x) =>
        CapacityType.M
      case x if (10 to 17).contains(x) =>
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
