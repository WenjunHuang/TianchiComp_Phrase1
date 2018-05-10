package cn.goldlokedu.alicomp.consumer.routers

import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{formFields, path, _}
import akka.http.scaladsl.server.Route
import akka.pattern._
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ConsumerAgentRouter(agentActor: ActorRef)(implicit ec: ExecutionContext, timeout: Timeout) {
  val routers: Route =
    (pathPrefix("invoke") &
      post &
      formFields('interface.as[String], 'method.as[String], 'parameterTypesString.as[String], 'parameter.as[String])) {
      (intr, method, pt, param) =>
        val f = agentActor ? BenchmarkRequest(
          requestId = UUID.randomUUID().getLeastSignificantBits,
          interface = intr,
          method = method,
          parameterTypeString = pt,
          parameter = param
        )


        onComplete(f) {
          case Success(BenchmarkResponse(_, _, Some(result))) =>
            complete(StatusCodes.OK -> result.toString)
          case Success(_) =>
            complete(StatusCodes.InternalServerError)
          case Failure(cause) =>
            complete(StatusCodes.InternalServerError)
        }

    }
}
