package cn.goldlokedu.alicomp

import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route

trait HttpServer {
  this: FrontendRouter with AkkaInfrastructure with SystemConfiguration ⇒

  val routers: Route = ???
  Http().bindAndHandle(routers, consumerHttpHost, consumerHttpPort)
}