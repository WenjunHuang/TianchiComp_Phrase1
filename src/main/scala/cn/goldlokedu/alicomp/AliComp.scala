package cn.goldlokedu.alicomp

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.ConfigFactory

trait AliComp extends Actors
  with FrontendRouter
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
  override def runAsProviderSmallAgent(): Unit = ???

  override def runAsConsumerAgent(): Unit = {
    val routers: Route = ???
    Http().bindAndHandle(routers, consumerHttpHost, consumerHttpPort)
  }

  override def runAsProviderMediumAgent(): Unit = ???

  override def runAsProviderLargeAgent(): Unit = ???
}

