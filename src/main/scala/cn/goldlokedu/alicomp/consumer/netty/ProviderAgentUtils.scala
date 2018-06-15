package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.CapacityType
import io.netty.channel.Channel
import io.netty.util.concurrent

object ProviderAgentUtils {
  def setProviderAgentChannel(cap: CapacityType.Value, channel: Channel) = {
    cap match {
      case CapacityType.L =>
        LargeAgent.set(channel)
      case CapacityType.M =>
        MediumAgent.set(channel)
      case CapacityType.S =>
        SmallAgent.set(channel)
    }
    println(s"ThreadId: ${Thread.currentThread().getId}, L:${LargeAgent.isSet}, M: ${MediumAgent.isSet}, S: ${SmallAgent.isSet}")
  }

  def getProviderAgentChannel(cap: CapacityType.Value): Channel = {
    cap match {
      case CapacityType.L =>
        LargeAgent.get
      case CapacityType.M =>
        MediumAgent.get
      case CapacityType.S =>
        SmallAgent.get
    }
  }

  private val LargeAgent = new concurrent.FastThreadLocal[Channel]()
  private val MediumAgent = new concurrent.FastThreadLocal[Channel]()
  private val SmallAgent = new concurrent.FastThreadLocal[Channel]()
}
