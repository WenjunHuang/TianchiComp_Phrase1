package cn.goldlokedu.alicomp.consumer.routers

import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

import akka.actor.{ActorRef, ActorSystem}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.pattern._
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}
import org.apache.commons.lang3.{RandomStringUtils, RandomUtils}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class ConsumerAgentRouter(agentRouter: ActorRef)(implicit ec: ExecutionContext,
                                                 timeout: Timeout,
                                                 logger: LoggingAdapter,
                                                 system: ActorSystem) {
  val routers: Route =
    (post &
      formFields('interface.as[String], 'method.as[String], 'parameterTypesString.as[String], 'parameter.as[String])) {
      (intr, method, pt, param) =>

        val f = (agentRouter ? BenchmarkRequest(
          requestId = UUID.randomUUID().getLeastSignificantBits,
          interface = intr,
          method = method,
          parameterTypeString = pt,
          parameter = param)).mapTo[BenchmarkResponse]

        onComplete(f) {
          case Success(BenchmarkResponse(_, _, Some(result))) =>
            complete(StatusCodes.OK -> result.toString)
          case Success(BenchmarkResponse(_, status, _)) =>
            logger.error(s"dubbo return status: $status")
            complete(StatusCodes.InternalServerError)
          case Failure(cause) =>
            logger.error(cause, s"error in connect provider")
            complete(StatusCodes.InternalServerError)
        }

    }

  val invoke: Route = (pathPrefix("invoke") & get) {
      val r = ThreadLocalRandom.current()
      val str = RandomStringUtils.random(r.nextInt(1024), true, true)
      val f = (agentRouter ? BenchmarkRequest(
        requestId = UUID.randomUUID().getLeastSignificantBits,
        interface = "com.alibaba.dubbo.performance.demo.provider.IHelloService",
        method = "hash",
        parameterTypeString = "Ljava/lang/String;",
        parameter = str)).mapTo[BenchmarkResponse]

      onComplete(f) {
        case Success(BenchmarkResponse(_, _, Some(result))) =>
          complete(StatusCodes.OK -> result.toString)
        case Success(BenchmarkResponse(_, status, _)) =>
          logger.error(s"dubbo return status: $status")
          complete(StatusCodes.InternalServerError)
        case Failure(cause) =>
          logger.error(cause, s"error in connect provider")
          complete(StatusCodes.InternalServerError)
      }
    }
}
