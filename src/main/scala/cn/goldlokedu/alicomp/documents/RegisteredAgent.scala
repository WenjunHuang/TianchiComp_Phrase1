package cn.goldlokedu.alicomp.documents

import akka.actor.ActorPath
import cn.goldlokedu.alicomp.documents.CapacityType.CapacityType
import cn.goldlokedu.alicomp.util.json.EnumJsonConverter
import spray.json.RootJsonFormat
import cn.goldlokedu.alicomp.util.json.ServiceProtocol._

object CapacityType extends Enumeration {
  type CapacityType = Value
  val S: Value = Value(1)
  val M: Value = Value(2)
  val L: Value = Value(3)

  implicit val CapacityTypeFormat: EnumJsonConverter[CapacityType.type] = new EnumJsonConverter(CapacityType)
}

/**
  *
  * @param cap 容量类型
  * @param agentName 代理名字， 必须唯一
  * @param address 代理的akka ref 地址
  */
case class RegisteredAgent(cap: CapacityType, agentName: String, address: String)
object RegisteredAgent {
  implicit val RegisteredAgentFormat: RootJsonFormat[RegisteredAgent] = jsonFormat3(RegisteredAgent.apply)
}
