package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.{DequeBasedMessageQueueSemantics, Envelope, PriorityGenerator, UnboundedStablePriorityMailbox}
import cn.goldlokedu.alicomp.provider.actors.DubboActor.DoneWrite
import com.typesafe.config.Config

class DubboActorMailBox(settings: ActorSystem.Settings, config: Config) extends UnboundedStablePriorityMailbox(
  PriorityGenerator {
    case DoneWrite => 0
    case _ => 1
  })
