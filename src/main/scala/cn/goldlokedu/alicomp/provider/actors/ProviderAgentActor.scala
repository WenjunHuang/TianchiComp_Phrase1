package cn.goldlokedu.alicomp.provider.actors

import akka.actor.{Actor, Props, Status}
import akka.event.LoggingAdapter
import akka.pattern._
import akka.routing._
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient
import cn.goldlokedu.alicomp.util.GetActorRemoteAddressExtension

import scala.concurrent.ExecutionContext

class ProviderAgentActor(capType: CapacityType.Value,
                         dubboActorCount: Int,
                         threhold: Int,
                         dubboHost: String,
                         dubboPort: Int)(implicit etcdClient: EtcdClient,
                                         logger: LoggingAdapter) extends Actor {

  implicit val ec: ExecutionContext = context.system.dispatcher

  import ProviderAgentActor._

  var router = {
    val routees = Vector.fill(dubboActorCount) {
      val r = context.actorOf(Props(new DubboActor(dubboHost, dubboPort, threhold)))
      context watch r
      ActorRefRoutee(r)
    }
    Router(RoundRobinRoutingLogic(), routees)
  }

  override def preStart(): Unit = {
    self ! Init
  }

  override def receive: Receive = init

  private def init: Receive = {
    case Init =>
      initialize()
    case PublishedToEtcd =>
      logger.info("address published to etcd")
      context become ready
    case Status.Failure(cause) =>
      logger.error(s"can not publish tot etcd", cause)
      context stop self
  }

  private def ready: Receive = {
    case r: BenchmarkRequest =>
      router.route(r, sender())
  }


  def initialize() = {
    //debug
    val address = GetActorRemoteAddressExtension(context.system).remotePath(self.path)
    logger.info(address.toString)
    etcdClient.addProvider(RegisteredAgent(capType, address.toString, address.toString))
      .map(_ => PublishedToEtcd) pipeTo self
  }
}

object ProviderAgentActor {

  case object Init

  case object PublishedToEtcd

}
