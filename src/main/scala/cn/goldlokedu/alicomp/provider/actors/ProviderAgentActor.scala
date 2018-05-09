package cn.goldlokedu.alicomp.provider.actors

import akka.actor._
import akka.event.LoggingAdapter
import akka.routing.SmallestMailboxPool
import cn.goldlokedu.alicomp.documents._
import cn.goldlokedu.alicomp.etcd.EtcdClient

class ProviderAgentActor(capType: CapacityType.Value,
                         dubboActorCount: Int,
                         threhold: Int,
                         dubboHost: String,
                         dubboPort: Int)(implicit etcdClient: EtcdClient,
                                         logger: LoggingAdapter) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    val name = self.path.name
    val path = self.path

    val registeredAgent =
      RegisteredAgent(capType, name, path.toStringWithoutAddress)
    etcdClient.addProvider(registeredAgent)
  }

  val dubbo: ActorRef =
    context.actorOf(Props(new DubboActor(dubboHost, dubboPort, threhold))
                    .withRouter(
                      new SmallestMailboxPool(dubboActorCount)
                        .withSupervisorStrategy(SupervisorStrategy.defaultStrategy)))

  override def receive: Receive = {

    case request: BenchmarkRequest =>
      dubbo.forward(request)

    case response: BenchmarkResponse =>
      sender ! response

    case Terminated(child) =>
      log.debug("{} has been terminated", child.path)
  }

}
