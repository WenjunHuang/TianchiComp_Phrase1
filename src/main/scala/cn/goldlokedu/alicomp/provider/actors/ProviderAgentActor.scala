package cn.goldlokedu.alicomp.provider.actors

import akka.actor._
import akka.event.LoggingAdapter
import akka.routing.SmallestMailboxPool
import cn.goldlokedu.alicomp.documents.CapacityType.CapacityType
import cn.goldlokedu.alicomp.documents._
import cn.goldlokedu.alicomp.etcd.EtcdClient

class ProviderAgentActor(dubboHost: String,
                         dubboPort: Int,
                         threhold: Int,
                         capacityType: CapacityType)(implicit client: EtcdClient, logger: LoggingAdapter) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    val name = self.path.name
    val path = self.path

    val registeredAgent =
      RegisteredAgent(capacityType, name, path)
    client.addProvider(registeredAgent)
  }

  val dubbo: ActorRef =
    context.actorOf(Props(new DubboActor(dubboHost, dubboPort, threhold))
                    .withRouter(
                      new SmallestMailboxPool(8)
                        .withSupervisorStrategy(SupervisorStrategy.defaultStrategy)))

  override def receive: Receive = {

    case request: BenchmarkRequest =>
      dubbo.forward(request)

    case response: BenchmarkResponse =>
      sender ! response
  }

}
