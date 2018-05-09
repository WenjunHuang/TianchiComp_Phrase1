package cn.goldlokedu.alicomp.etcd

import cn.goldlokedu.alicomp.documents.RegisteredAgent
import org.etcd4s.pb.mvccpb.KeyValue
import spray.json._
import org.etcd4s.{Etcd4sClient, Etcd4sClientConfig}
import org.etcd4s.implicits._
import org.etcd4s.formats.Formats._
import cn.goldlokedu.alicomp.util.json.ServiceProtocol._

import scala.concurrent.{ExecutionContext, Future}

class EtcdClient(host: String, port: Int)(implicit dispatcher: ExecutionContext) {
  private val providerPath = "providers/"
  private val client = Etcd4sClient.newClient(Etcd4sClientConfig(address = host, port = port))

  def addProvider(agent: RegisteredAgent): Future[Option[KeyValue]] = {
    client.kvService.setKey(s"$providerPath${agent.agentName}", agent.toJson.compactPrint)
  }

  def providers(): Future[Seq[RegisteredAgent]] = {
    client.kvService.getRange(providerPath) map { elem =>
      elem.kvs.map { kv =>
        kv.value.toStringUtf8.parseJson.convertTo[RegisteredAgent]
      }
    }
  }
}
