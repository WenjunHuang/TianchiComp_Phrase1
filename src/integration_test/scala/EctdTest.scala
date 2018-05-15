import akka.actor.ActorPath
import cn.goldlokedu.alicomp.documents.{CapacityType, RegisteredAgent}
import cn.goldlokedu.alicomp.etcd.EtcdClient

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

object EctdTest {
  def main(args: Array[String]): Unit = {
    val client = new EtcdClient("192.168.2.248", 2379)
    val ret = client.addProvider(RegisteredAgent(CapacityType.L, "test", "tcp://11111/xxxx",80))
    val result = Await.result(ret, 10.seconds)
    println(result)

    val p = client.providers()
    val resultp = Await.result(p, 10.seconds)
    println(resultp)
  }
}
