package cn.goldlokedu.alicomp.consumer

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.stream.scaladsl.Sink
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.http.scaladsl.server.Directives._

import scala.util.{Success, Try}

class ConsumerAgentHttpServer(consumerHttpHost: String,
                              consumerHttpPort: Int,
                              agentRouter: ActorRef)(implicit system: ActorSystem, ec: ExecutionContext, materializer: ActorMaterializer, timeout: Timeout) {
  lazy val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(POST, _, _, entity, _) =>
      PredefinedFromEntityUnmarshallers.defaultUrlEncodedFormDataUnmarshaller(entity)
        .flatMap { formData =>
          val intr = formData.fields.get("interface").get
          val method = formData.fields.get("method").get
          val pts = formData.fields.get("parameterTypesString").get
          val param = formData.fields.get("parameter").get

          val double =
            (agentRouter ? BenchmarkRequest(
              requestId = UUID.randomUUID().getLeastSignificantBits,
              interface = intr,
              method = method,
              parameterTypeString = pts,
              parameter = param))
              .mapTo[Try[BenchmarkResponse]]

          double.map {
            case Success(result) if result.status == 20 =>
              HttpResponse(200, entity = String.valueOf(result.result.get))
            case _ =>
              HttpResponse(500)
          }
        }

  }

  //  val requestRoute:Route = (post & formFields('interface.as[String],'method.as[String],'parameterTypesString.as[String],'parameter.as[String])) {
  //
  //  }
  def run() = {
    val serverSource = Http().bindAndHandleAsync(requestHandler,consumerHttpHost, consumerHttpPort)
//    serverSource.to(Sink.foreach { connection =>
//      connection.handleWithAsyncHandler(requestHandler)
//    }).run()
  }
}
