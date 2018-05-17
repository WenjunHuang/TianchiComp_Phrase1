package cn.goldlokedu.alicomp


import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.util.Timeout
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._

trait AkkaInfrastructure {
  this: Configuration with SystemConfiguration ⇒

  implicit lazy val system: ActorSystem = ActorSystem("AliComp", config)
  implicit lazy val executionContext: ExecutionContextExecutor = system.dispatcher
  implicit lazy val logger: LoggingAdapter = system.log

  implicit def etcdClient: EtcdClient = new EtcdClient(etcdHost, etcdPort)

  implicit lazy val timeout: Timeout = Timeout(2 seconds)

  sys.addShutdownHook(system.terminate())
}

trait Actors {
  this: AkkaInfrastructure =>
}