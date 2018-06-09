package cn.goldlokedu.alicomp

import cn.goldlokedu.alicomp.consumer.ConsumerAgentNettyHttpServer
import cn.goldlokedu.alicomp.documents.CapacityType
import cn.goldlokedu.alicomp.provider.netty.ProviderAgentServer
import com.typesafe.config.ConfigFactory

trait AliComp extends Infrastructure
  with Configuration
  with SystemConfiguration
  with ProviderConfiguration
  with ConsumerConfiguration {

  override def config = {
    Option(System.getProperty("RUN_TYPE")) match {
      case Some(runType) =>
        ConfigFactory.load(runType)
          .withFallback(ConfigFactory.load())
      case None =>
        throw new IllegalStateException("please provide run type")
    }
  }

  def runConsumerAgent(name: String) = {
    logger.info(s"run as lowlevel $name")
    val server = new ConsumerAgentNettyHttpServer(etcdClient, consumerHttpHost, consumerHttpPort)
    server.run()
  }

  def startProvider(cap: CapacityType.Value, name: String): Unit = {
    logger.info(s"run as $name")
    val server = new ProviderAgentServer(
      providerHost,
      cap,
      name,
      dubboProviderHost,
      dubboProviderPort)
    server.run()
  }


  runType match {
    case name@"provider-small" => startProvider(CapacityType.S, name)
    case name@"provider-medium" => startProvider(CapacityType.M, name)
    case name@"provider-large" => startProvider(CapacityType.L, name)
    case name@"consumer" => runConsumerAgent(name) //runAsConsumerAgent(name)
    case _ =>
      throw new IllegalArgumentException("don't known which type i should run as.(provider/consumer)")
  }
}

object Boot extends App with AliComp {
}

