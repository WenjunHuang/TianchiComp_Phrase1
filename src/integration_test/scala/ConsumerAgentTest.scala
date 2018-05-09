import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.util.Timeout
import cn.goldlokedu.alicomp.consumer.{ConsumerAgentActor, ProviderAgentActor}
import cn.goldlokedu.alicomp.consumer.route.ConsumerRoute
import cn.goldlokedu.alicomp.etcd.EctdClient

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object ConsumerAgentTest {
  implicit val system = ActorSystem("ConsumerAgentTest")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val logger = system.log
  implicit val ec = system.dispatcher
  implicit val timeout: Timeout = Timeout(5.seconds)
  val client = new EctdClient("192.168.2.248", 2379)

  private val providerAgentActorsFuture = client.providers() flatMap { providers =>
    println(providers)
    Future.sequence(providers map { provider => system.actorSelection(provider.address).resolveOne() map { (provider.cap, _) } })
  } map { refs =>
    refs map { ref => ProviderAgentActor(ref._1, ref._2) }
  }

  private val providerAgentActors = Await.result(providerAgentActorsFuture, 5.seconds)
  private val actor = system.actorOf(Props(classOf[ConsumerAgentActor], providerAgentActors))
  private val routers: Route = new ConsumerRoute(actor).invoke

  def main(args: Array[String]): Unit = {
    Http().bindAndHandle(routers, "0.0.0.0", 8090)
  }
}
