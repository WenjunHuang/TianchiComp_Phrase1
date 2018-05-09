package cn.goldlokedu.alicomp.consumer

import akka.actor.{Actor, ActorLogging}
import cn.goldlokedu.alicomp.documents.{BenchmarkRequest, BenchmarkResponse}

class ConsumerAgentActor(providers: Array[ProviderAgentActor]) extends Actor with ActorLogging {
  private val size = providers.length - 1
  private var cursor = 0

  override def receive: Receive = {
    case request: BenchmarkRequest =>
      getProvider(cursor).ref.forward(BenchmarkResponse(request.requestId, 20, Some(20)))
      if (cursor == size) cursor = 0 else cursor = cursor + 1
  }

  private def getProvider(cursor: Int) = {
    providers(cursor)
  }
}
