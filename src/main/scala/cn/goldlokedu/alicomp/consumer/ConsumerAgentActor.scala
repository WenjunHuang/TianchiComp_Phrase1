package cn.goldlokedu.alicomp.consumer

import akka.actor.Status.Failure
import akka.actor._
import akka.util.Timeout
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.concurrent.Future

class ConsumerAgentActor(client: EtcdClient)(implicit timeout: Timeout) extends Actor with ActorLogging {
  import ConsumerAgentActor._
  import akka.pattern.pipe
  import context.dispatcher

  private var signal: Cancellable = _
  private var providers: Seq[RegisteredAgent] = Seq.empty
  private var provider: Option[ActorRef] = None

  override def preStart(): Unit = {
    schedule()
    super.preStart()
  }

  override def receive: Receive = {
    case Tick =>
      client.providers() pipeTo self

    case p: Seq[RegisteredAgent @unchecked] =>
      providers = providers ++ p
      Future
        .traverse(providers
          .map{agent =>
            context.actorSelection(agent.address).resolveOne()})(identity) pipeTo self

    case Failure(f) =>
      log.debug(f.getMessage)
      schedule()

    case providers: Seq[ActorRef @unchecked] =>
      provider  = providers.headOption

    case Terminated(remoteActorRef) =>
      log.debug("{} on remote on work", remoteActorRef.path)

    case request: BenchmarkRequest =>
      provider.foreach(_.tell(request, sender))

    case response: BenchmarkResponse =>
      sender() ! response
  }

  protected def schedule() {
    cancel()
    signal = context.system.scheduler.scheduleOnce(timeout.duration, self, Tick)
  }

  protected def cancel(): Unit = if (signal != null) signal.cancel()

}

object ConsumerAgentActor {
  case object Tick
}
