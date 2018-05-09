package cn.goldlokedu.alicomp


import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import cn.goldlokedu.alicomp.etcd.EtcdClient
import scala.concurrent.duration._

import scala.concurrent.ExecutionContextExecutor

trait AkkaInfrastructure {
  this: Configuration with SystemConfiguration ⇒

  implicit lazy val system: ActorSystem = ActorSystem("AliComp", config)
  implicit lazy val materializer: ActorMaterializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit lazy val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit lazy val logger: LoggingAdapter = system.log
  implicit lazy val etcdClient: EtcdClient = new EtcdClient(etcdHost, etcdPort)
  implicit lazy val timeout = Timeout(5 seconds)

  sys.addShutdownHook(system.terminate())
}

trait Actors {
  this: AkkaInfrastructure =>
}