package cn.goldlokedu.alicomp.consumer.actors

import java.time.Period

import akka.actor.{Actor, ActorRef}
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class MilestoneActor(implicit ec:ExecutionContext) extends Actor {

  import MilestoneActor._

  val senders: mutable.Map[(ActorRef, Long, Int), Duration] = mutable.Map.empty

  override def preStart(): Unit = {
    context.system.scheduler.schedule(5 milliseconds, 5 milliseconds, self, Tick)
  }

  override def receive: Receive = {
    case Tick =>
      senders.keys.foreach { it =>
        senders.get(it) match {
          case Some(p) =>
            val n = p - (5 milliseconds)
            if (n < 0.seconds) {
              senders.remove(it)
              it._1 ! BenchmarkResponse(it._2, 20, Some(it._3))
            } else {
              senders(it) = n
            }
          case None =>
        }
      }
    case r: BenchmarkRequest =>
      senders((sender, r.requestId, r.parameter.hashCode)) = 50 milliseconds

  }
}

object MilestoneActor {

  case object Tick

}
