import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}
import cn.goldlokedu.alicomp.provider.actors.DubboActor
import akka.pattern._
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
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
  val host = "192.168.2.221"
  val port1 = 20890
  val port2 = 20891
  val port3 = 20892


  val dubboActors = Seq(
    system.actorOf(Props(classOf[DubboActor], host, port1, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port1, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port1, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port1, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port2, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port2, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port2, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port2, 50, logger)), //.withMailbox("prio-dispatcher")),
    system.actorOf(Props(classOf[DubboActor], host, port3, 50, logger)), //.withMailbox("prio-dispatcher"))
    system.actorOf(Props(classOf[DubboActor], host, port3, 50, logger)), //.withMailbox("prio-dispatcher"))
    system.actorOf(Props(classOf[DubboActor], host, port3, 50, logger)), //.withMailbox("prio-dispatcher"))
    system.actorOf(Props(classOf[DubboActor], host, port3, 50, logger))
  )


  def routers: Route =
    (post & formFields('value.as[String])) { value =>
      val request = BenchmarkRequest(
        UUID.randomUUID().getMostSignificantBits,
        "com.alibaba.dubbo.performance.demo.provider.IHelloService",
        "hash",
        "Ljava/lang/String;",
        value)
      val index = ThreadLocalRandom.current().nextInt(dubboActors.size)
      val fut = (dubboActors(index) ? request).mapTo[BenchmarkResponse]
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
