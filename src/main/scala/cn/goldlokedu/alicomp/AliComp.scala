package cn.goldlokedu.alicomp

import akka.actor.Props
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import cn.goldlokedu.alicomp.consumer.actors.ConsumerAgentActor
import cn.goldlokedu.alicomp.consumer.routers.ConsumerAgentRouter
import cn.goldlokedu.alicomp.documents.CapacityType
import cn.goldlokedu.alicomp.provider.actors.ProviderAgentActor
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait AliComp extends Actors
  with AkkaInfrastructure
  with Configuration
  with SystemConfiguration
  with ProviderConfiguration
  with ConsumerConfiguration {
  override def config = ConfigFactory.load()

  def runAsProviderSmallAgent(): Unit = {
    startProvider(CapacityType.S)
  }

  def runAsProviderMediumAgent(): Unit = {
    startProvider(CapacityType.M)
  }

  def runAsProviderLargeAgent(): Unit = {
    startProvider(CapacityType.L)
  }

  def runAsConsumerAgent(): Unit = {
    val providerAgentActorsFuture = etcdClient.providers() flatMap { providers =>
      Future.sequence(providers map { provider =>
        system.actorSelection(provider.address).resolveOne() map {
          (provider.cap, _)
        }
      })
    }

    providerAgentActorsFuture.onComplete {
      case Success(providerAgentActors) =>
        val actor = system.actorOf(Props(new ConsumerAgentActor(Map(providerAgentActors: _*))))
        val router1: Route = new ConsumerAgentRouter(actor).routers
        Http().bindAndHandle(router1, consumerHttpHost, consumerHttpPort)
      case Failure(ex) =>

    }
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

