package cn.goldlokedu.alicomp.consumer

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import akka.stream.scaladsl.Sink
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}

import scala.concurrent.{ExecutionContext, Future}
import akka.pattern._
import akka.stream.ActorMaterializer
import akka.util.Timeout

class LowLevelHttpHandler(consumerHttpHost: String,
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

          (agentRouter ? BenchmarkRequest(
            requestId = UUID.randomUUID().getLeastSignificantBits,
            interface = intr,
            method = method,
            parameterTypeString = pts,
            parameter = param))
            .mapTo[BenchmarkResponse]
            .map { response =>
              response.result match {
                case Some(result) =>
                  HttpResponse(200, entity = String.valueOf(response.result.get))
                case None =>
                  HttpResponse(500)
              }
            }
        }

  }

  lazy val serverSource = Http().bind(consumerHttpHost, consumerHttpPort)
  val bindingFuture: Future[Http.ServerBinding] = serverSource.to(Sink.foreach { connection =>
    connection.handleWithAsyncHandler(requestHandler,1000)
  }).run()
}
