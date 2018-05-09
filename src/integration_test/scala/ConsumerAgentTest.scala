import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import cn.goldlokedu.alicomp.consumer.ConsumerAgentActor
import cn.goldlokedu.alicomp.consumer.route.ConsumerRoute
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.concurrent.duration._

object ConsumerAgentTest {
  implicit val system           = ActorSystem("ConsumerAgentTest")
  implicit val materializer     = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val logger           = system.log
  implicit val ec               = system.dispatcher
  implicit val timeout: Timeout = Timeout(5.seconds)
  val client = new EtcdClient("192.168.2.248", 2379)

  private val actor          = system.actorOf(Props(new ConsumerAgentActor(client)))
  private val routers: Route = new ConsumerRoute(actor).invoke

  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(routers, "0.0.0.0", 8090)
  }
}
