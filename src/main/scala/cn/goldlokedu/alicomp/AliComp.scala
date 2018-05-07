package cn.goldlokedu.alicomp

import com.typesafe.config.ConfigFactory

trait AliComp extends Actors
  with FrontendRouter
  with HttpServer
  with AkkaInfrastructure
  with Configuration
  with SystemConfiguration {
  override def config = ConfigFactory.load()
}

object Boot extends App with AliComp

