import akka.actor.{ActorSystem, Props}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.CapacityType
import cn.goldlokedu.alicomp.etcd.EtcdClient
import cn.goldlokedu.alicomp.provider.actors.ProviderAgentActor

object ProviderAgentActorTest extends App {
  implicit val system = ActorSystem("Test")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val logger = system.log
  implicit val ec = system.dispatcher

  val etcdClient = new EtcdClient("192.168.2.248", 2379)
  val test = system.actorOf(Props(new ProviderAgentActor(CapacityType.L,2, 50,"192.168.2.248",20890)(etcdClient, logger)),"providerAgent")

}
