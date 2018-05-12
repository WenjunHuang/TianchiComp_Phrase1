package cn.goldlokedu.alicomp.consumer.actors

import akka.actor.{Actor, Props}
import akka.event.LoggingAdapter
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.util.Timeout
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.concurrent.ExecutionContext

class ConsumerAgentActorRouter(agentCount: Int)(implicit ec: ExecutionContext,
                                                to: Timeout,
                                                logger: LoggingAdapter,
                                                etcdClient: EtcdClient) extends Actor {

  var agentRouter = {
    val routees = Vector.fill(agentCount) {
      val r = context.actorOf(Props(new ConsumerAgentActor))
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive: Receive = {
    case any =>
      agentRouter.route(any, sender)
  }
}
