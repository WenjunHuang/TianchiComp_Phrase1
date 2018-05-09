package cn.goldlokedu.alicomp.provider.actors

import akka.actor.Status.Failure
import akka.actor._
import cn.goldlokedu.alicomp.documents.CapacityType.CapacityType
import cn.goldlokedu.alicomp.documents._
import cn.goldlokedu.alicomp.etcd.EctdClient

import scala.concurrent.duration.FiniteDuration

class ProviderAgentActor(client: EctdClient,
                         capacityType: CapacityType,
                         duration: FiniteDuration) extends Actor with ActorLogging {

  import akka.pattern.pipe
  import context.dispatcher
  import ProviderAgentActor._

  private var signal: Cancellable = _
  private var consumer: Option[ActorSelection] = None

  override def preStart(): Unit = {
    val name = self.path.name
    val path = self.path

    val registeredAgent =
      RegisteredAgent(capacityType, name, path)

    client.addConsumer(registeredAgent)
  }

  override def receive: Receive = {
    case Tick =>
      client.consumers() pipeTo self
      cancel()

    case Failure(f) =>
      log.debug("retry for remote provider agent, exception: {}", f)
      schedule()

    case consumerPath: Option[RegisteredAgent @unchecked] =>
      consumer =
        consumerPath.map (c => context.actorSelection(c.address))

    case request: BenchmarkRequest =>
      sender ! request

    case response: BenchmarkResponse =>
      consumer.foreach(_ ! response)
  }

  protected def schedule() {
    cancel()
    signal = context.system.scheduler.scheduleOnce(duration, self, Tick)
  }

  protected def cancel(): Unit = if (signal != null) signal.cancel()

}

object ProviderAgentActor {
  case object Tick
}