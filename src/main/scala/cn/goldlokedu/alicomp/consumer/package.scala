package cn.goldlokedu.alicomp

import akka.actor.ActorRef
import cn.goldlokedu.alicomp.documents.CapacityType.CapacityType

package object consumer {
  case class ProviderAgentActor(cap: CapacityType, ref: ActorRef)
}
