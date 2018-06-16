package cn.goldlokedu.alicomp.consumer.netty

import cn.goldlokedu.alicomp.documents.CapacityType
import io.netty.channel.Channel
import io.netty.util.concurrent
import io.netty.util.concurrent.FastThreadLocal

object ProviderAgentUtils {
  val MaxRoll = 13
  //  val largeBound = Set(0, 1, 3, 4, 6, 7, 9, 10, 12)
  val largeBound = Set(0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12)
  //  val mediumBound = Set(2, 5, 8, 11)
  val mediumBound = Nil
  val smallBound = Nil

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

  def chooseProviderAgent(): Channel = {
    val roll = if (Roll.isSet) {
      val v = Roll.get() + 1
      Roll.set(v)
      v
    } else {
      Roll.set(0)
      0
    }

    val cap = roll % MaxRoll match {
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
  private val Roll = new FastThreadLocal[Int]()
}
