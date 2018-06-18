package cn.goldlokedu.alicomp


import cn.goldlokedu.alicomp.etcd.EtcdClient
import org.slf4j._

import scala.concurrent.ExecutionContext.Implicits._

trait Infrastructure {
  this: Configuration with SystemConfiguration ⇒

  implicit lazy val etcdClient: EtcdClient = new EtcdClient(etcdHost, etcdPort)

  implicit lazy val logger = LoggerFactory.getLogger("cn.goldlokedu.alicomp")

}

