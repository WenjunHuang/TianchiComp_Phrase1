package cn.goldlokedu.alicomp


import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent.ExecutionContextExecutor

trait AkkaInfrastructure {
  this: Configuration ⇒

  implicit lazy val system: ActorSystem = ActorSystem("AliComp", config)
  implicit lazy val materializer:ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit lazy val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit lazy val logger: LoggingAdapter = system.log

  sys.addShutdownHook(system.terminate())
}

trait Actors {
  this: AkkaInfrastructure =>
}