package cn.goldlokedu.alicomp.provider.actors

import java.net.InetSocketAddress

import akka.actor.{Actor, Props}
import akka.event.LoggingAdapter
import akka.io.{IO, Tcp}
import cn.goldlokedu.alicomp.documents.{CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.concurrent.ExecutionContext

class DubboTcpServer(serverHost: String,
                     cap: CapacityType.Value,
                     name: String,
                     dubboActorCount: Int,
                     threhold: Int,
                     dubboHost: String,
                     dubboPort: Int)(implicit etcdClient: EtcdClient,
                                     logger: LoggingAdapter,
                                     ec: ExecutionContext) extends Actor {

  import Tcp._
  import context.system


  IO(Tcp) ! Bind(self, new InetSocketAddress(serverHost, 0))

  override def receive: Receive = {
    case b@Bound(localAddress) =>
      logger.info(s"bound to ${localAddress.toString}")
      context.parent ! b
      etcdClient.addProvider(RegisteredAgent(cap, name, localAddress.getHostName, localAddress.getPort))
        .onComplete(_ => etcdClient.shutdown())
    case CommandFailed(_: Bind) =>
      logger.error(s"can not bind to address")
    case c@Connected(remote, local) =>
      val connection = sender
      val handler = context.actorOf(Props(new DubboTcpClient(connection, dubboHost, dubboPort)))
      connection ! Register(handler)
  }
}
