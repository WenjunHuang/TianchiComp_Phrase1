package cn.goldlokedu.alicomp.provider.actors

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedStablePriorityMailbox}
import akka.io.Tcp.Received
import cn.goldlokedu.alicomp.provider.actors.DubboActor.DoneWrite
import com.typesafe.config.Config

class DubboActorPriorityMailbox(settings:ActorSystem.Settings,config:Config) extends UnboundedStablePriorityMailbox(
  PriorityGenerator{
    case DoneWrite => 0
    case Received(_) => 1
    case _ => 2
  }
)

