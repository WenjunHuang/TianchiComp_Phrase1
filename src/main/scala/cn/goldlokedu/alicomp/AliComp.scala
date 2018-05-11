package cn.goldlokedu.alicomp

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
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

  override def config = {
    Option(System.getProperty("RUN_TYPE")) match {
      case Some(runType) =>
        ConfigFactory.load(runType)
          .withFallback(ConfigFactory.load())
      case None =>
        throw new IllegalStateException("please provide run type")
    }
  }

  def runAsProviderSmallAgent(): Unit = {
    logger.info("run as provider small")
    startProvider(CapacityType.S)
  }

  def runAsProviderMediumAgent(): Unit = {
    logger.info("run as provider medium")
    startProvider(CapacityType.M)
  }

  def runAsProviderLargeAgent(): Unit = {
    logger.info("run as provider large")
    startProvider(CapacityType.L)
  }

  def runAsConsumerAgent(): Unit = {
    logger.info("run as consumer")
    val actor = system.actorOf(Props(new ConsumerAgentActor))
    val consumerRoute: Route = new ConsumerAgentRouter(actor).routers
    Http().bindAndHandle(consumerRoute, consumerHttpHost, consumerHttpPort)
  }

  def startProvider(cap: CapacityType.Value): Unit = {
    system.actorOf(Props(new ProviderAgentActor(
      cap,
      dubboProviderConnectionCount,
      dubboProviderMaxConcurrentCountPerConnection,
      dubboProviderHost,
      dubboProviderPort)))
  }


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
}

