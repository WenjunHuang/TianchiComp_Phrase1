package cn.goldlokedu.alicomp.consumer.vertx

import java.util.UUID

import akka.actor.ActorRef
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}
import io.vertx.lang.scala.ScalaVerticle
import io.vertx.scala.ext.web.handler.BodyHandler
import io.vertx.scala.ext.web.{Route, Router}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class HttpVerticle(consumerAgent: ActorRef, host: String, port: Int)(implicit timeout: Timeout) extends ScalaVerticle {

  override def startFuture(): Future[_] = {
    val router = Router.router(vertx)

    val route: Route = router.post()
      .handler(BodyHandler.create())
      .handler { ctx =>
        val request = ctx.request()
        val intr = request.getFormAttribute("interface").get
        val method = request.getFormAttribute("method").get
        val paramTypeStr = request.getFormAttribute("parameterTypesString").get
        val param = request.getParam("parameter").get
        val requestId = UUID.randomUUID().getLeastSignificantBits

        val benchReq = BenchmarkRequest(requestId, intr, method, paramTypeStr, param)
        (consumerAgent ? benchReq).mapTo[BenchmarkResponse].onComplete {
          case Success(response) if response.status == 20 =>
            ctx.response().end(response.result.get.toString)
          case _ =>
            ctx.response().setStatusCode(500)
        }
        //        ctx.response().end(intr.get)
      }

    vertx.createHttpServer()
      .requestHandler(router.accept(_))
      .listenFuture(port, host)
  }

}
