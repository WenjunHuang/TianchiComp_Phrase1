package cn.goldlokedu.alicomp.consumer.netty

import java.util.function.Supplier

import cn.goldlokedu.alicomp.documents
import cn.goldlokedu.alicomp.documents.CapacityType
import cn.goldlokedu.alicomp.documents.CapacityType.CapacityType
import io.netty.channel.Channel

import scala.collection.mutable

object ProviderAgentUtils {
  def setProviderAgentChannel(cap:CapacityType.Value,channel:Channel) = {
    AgentMap(cap) = channel
  }

  def getProviderAgentChannel(cap:CapacityType.Value):Channel = {
    AgentMap(cap)
  }

  private val AgentMap = ThreadLocal.withInitial(() => {
    mutable.Map[documents.CapacityType.Value, Channel]()
  })
}
