package cn.goldlokedu.alicomp.provider.actors

import akka.actor.ActorSystem
import akka.dispatch.{PriorityGenerator, UnboundedPriorityMailbox}
import akka.io.Tcp.Received
import com.typesafe.config.Config

class DubboPrioMailBox(settings:ActorSystem.Settings, config:Config) extends UnboundedPriorityMailbox(
  PriorityGenerator{
    case Received(_) => 0
    case _ => 1
  }
)

