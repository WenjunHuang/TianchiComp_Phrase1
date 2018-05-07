package cn.goldlokedu.alicomp

import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.duration._

trait FrontendRouter {
  this: Actors with AkkaInfrastructure ⇒

  implicit val timeout: Timeout = Timeout(5 seconds)

  //@formatter:off
  lazy val frontEndRoutes:Route = ???
}