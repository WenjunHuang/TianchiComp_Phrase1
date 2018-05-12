package cn.goldlokedu.alicomp

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import cn.goldlokedu.alicomp.consumer.actors.{ConsumerAgentActor, ConsumerAgentActorRouter}
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

  def runAsConsumerAgent(name: String): Unit = {
    logger.info(s"run as $name")
    val actor = system.actorOf(Props(new ConsumerAgentActorRouter(consumerAgentCount)), name)
    val consumerRoute: Route = new ConsumerAgentRouter(actor).milestone
    Http().bindAndHandle(consumerRoute, consumerHttpHost, consumerHttpPort)
  }

  def startProvider(cap: CapacityType.Value, name: String): Unit = {
    logger.info(s"run as $name")
    system.actorOf(Props(new ProviderAgentActor(
      cap,
      dubboProviderConnectionCount,
      dubboProviderMaxConcurrentCountPerConnection,
      dubboProviderHost,
      dubboProviderPort)), name)
  }


  runType match {
    case name@"provider-small" => startProvider(CapacityType.S, name)
    case name@"provider-medium" => startProvider(CapacityType.M, name)
    case name@"provider-large" => startProvider(CapacityType.L, name)
    case name@"consumer" => runAsConsumerAgent(name)
    case _ =>
      throw new IllegalArgumentException("don't known which type i should run as.(provider/consumer)")
  }
}

object Boot extends App with AliComp {
}

