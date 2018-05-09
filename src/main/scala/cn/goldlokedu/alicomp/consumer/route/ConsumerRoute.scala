package cn.goldlokedu.alicomp.consumer.route

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}

import scala.util._

class ConsumerRoute(actor: ActorRef) {
  implicit val timeout: Timeout = Timeout(5.seconds)
  val invoke: Route = path("invoke") {
    (post &
      formFields('interface.as[String],
        'method.as[String],
        'parameterTypesString.as[String],
        'parameter.as[String])) { (i, m, pts, p) =>
      val r = (actor ? BenchmarkRequest(UUID.randomUUID().getMostSignificantBits, i , m, pts, p)).mapTo[BenchmarkResponse]
      onComplete(r) {
        case Success(br) =>
          if (br.status == 20) complete(StatusCodes.OK, br.result.get.toString)
          else complete(StatusCodes.InternalServerError)
        case Failure(ex) => complete(StatusCodes.InternalServerError)
      }
    }
  }
}
