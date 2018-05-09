package cn.goldlokedu.alicomp

import akka.actor.Props
import akka.http.scaladsl.Http
import cn.goldlokedu.alicomp.consumer.actors.ConsumerAgentActor
import cn.goldlokedu.alicomp.consumer.routers.ConsumerAgentRouter
import cn.goldlokedu.alicomp.documents.CapacityType
import cn.goldlokedu.alicomp.provider.actors.ProviderAgentActor
import com.typesafe.config.ConfigFactory

trait AliComp extends Actors
  with AkkaInfrastructure
  with Configuration
  with SystemConfiguration
  with ProviderConfiguration
  with ConsumerConfiguration {
  override def config = ConfigFactory.load()


  def runAsProviderSmallAgent()

  def runAsProviderMediumAgent()

  def runAsProviderLargeAgent()

  def runAsConsumerAgent()

  runType match {
    case "provider-small" => runAsProviderSmallAgent()
    case "provider-medium" => runAsProviderMediumAgent()
    case "provider-large" => runAsProviderLargeAgent()
    case "consumer" => runAsConsumerAgent()
    case _ =>
      throw new IllegalArgumentException("don't known which type i should run as.(provider/consumer)")
  }
}

object Boot extends App with AliComp {
  override def runAsProviderSmallAgent(): Unit = {
    startProvider(CapacityType.S)
  }

  override def runAsProviderMediumAgent(): Unit = {
    startProvider(CapacityType.M)
  }

  override def runAsProviderLargeAgent(): Unit = {
    startProvider(CapacityType.L)
  }

  override def runAsConsumerAgent(): Unit = {
    val consumerAgent = system.actorOf(Props(new ConsumerAgentActor))
    val router = new ConsumerAgentRouter(consumerAgent)

    Http().bindAndHandle(router.routers, consumerHttpHost, consumerHttpPort)
  }

  private def startProvider(cap: CapacityType.Value): Unit = {
    system.actorOf(Props(new ProviderAgentActor(
      CapacityType.S,
      dubboProviderConnectionCount,
      dubboProviderMaxConcurrentCountPerConnection,
      dubboProviderHost,
      dubboProviderPort)))
  }
}

