package cn.goldlokedu.alicomp.consumer.actors

import akka.actor.{Actor, Props}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}

import scala.concurrent.ExecutionContext

class MilestoneActorRouter(implicit ec:ExecutionContext) extends Actor {
  var agentRouter = {
    val routees = Vector.fill(8) {
      val r = context.actorOf(Props(new MilestoneActor))
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def receive: Receive = {
    case any =>
      agentRouter.route(any, sender)
  }
}
