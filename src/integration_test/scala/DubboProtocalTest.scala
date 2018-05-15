import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{Actor, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}
import cn.goldlokedu.alicomp.provider.actors.DubboActor
import akka.pattern._
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object DubboProtocalTest extends App {


  implicit val system = ActorSystem("Test")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system))
  implicit val logger = system.log
  implicit val ec = system.dispatcher
  implicit val timeout = Timeout(5 seconds)
  val host1 = "localhost"
  val port1 = 20880


  val dubboActors = Vector(
    system.actorOf(Props(new DubboActor(host1, port1, 100))))

  class RouterActor extends Actor {
    val router = Router(RoundRobinRoutingLogic(), dubboActors.map(ActorRefRoutee(_)))

    override def receive: Receive = {
      case any =>
        router.route(any, sender)
    }
  }

  val router = system.actorOf(Props(new RouterActor))


  def routers: Route =
    (post & formFields('value.as[String])) { value =>
      val request = BenchmarkRequest(
        UUID.randomUUID().getMostSignificantBits,
        "com.alibaba.dubbo.performance.demo.provider.IHelloService",
        "hash",
        "Ljava/lang/String;",
        value)
      val index = ThreadLocalRandom.current().nextInt(dubboActors.size)
      val fut = (router ? request).mapTo[BenchmarkResponse]
      onComplete(fut) {
        case Success(msg) =>
          if (msg.status == 20)
            complete(StatusCodes.OK -> msg.result.get.toString)
          else {
            logger.error(s"get error code : ${msg.status}")
            complete(StatusCodes.InternalServerError)
          }
        case Failure(cause) =>
          //          logger.error(cause,s"server error")
          complete(StatusCodes.InternalServerError)
      }
    }

  Http().bindAndHandle(routers, "0.0.0.0", 8080)

}
