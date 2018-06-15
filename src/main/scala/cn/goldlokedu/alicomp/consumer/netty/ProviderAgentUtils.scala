package cn.goldlokedu.alicomp.consumer.netty

import java.util.concurrent.ThreadLocalRandom

import cn.goldlokedu.alicomp.documents.CapacityType
import io.netty.channel.Channel
import io.netty.util.concurrent

object ProviderAgentUtils {
  val MaxRoll = 13
  val largeBound = Set(0, 1, 3, 4, 5, 9, 10, 12)
  //  val largeBound = Set(0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12)
  val mediumBound = Set(2, 6, 7, 11)
  val smallBound = Set(8)

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

  def chooseProviderAgent():Channel = {
    val roll = ThreadLocalRandom.current().nextInt(MaxRoll)
    val cap = roll match {
      case x if largeBound contains x =>
        CapacityType.L
      case x if mediumBound contains x =>
        CapacityType.M
      case x if smallBound contains x =>
        CapacityType.S
      case _ =>
        CapacityType.L
    }
    getProviderAgentChannel(cap)
  }

  private val LargeAgent = new concurrent.FastThreadLocal[Channel]()
  private val MediumAgent = new concurrent.FastThreadLocal[Channel]()
  private val SmallAgent = new concurrent.FastThreadLocal[Channel]()
}
