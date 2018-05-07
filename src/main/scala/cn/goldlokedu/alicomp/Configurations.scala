package cn.goldlokedu.alicomp

import com.typesafe.config.Config

trait Configuration {
  def config: Config
}

// 系统配置参数
trait SystemConfiguration {
  this: Configuration ⇒
  private def systemConfig = config.getConfig("system")

  def actorSystemHost = systemConfig.getString("actor-system-host")

  def actorSystemPort = systemConfig.getInt("actor-system-port")

  def consumerHttpHost = systemConfig.getString("consumer-http-host")

  def consumerHttpPort = systemConfig.getInt("consumer-http-port")

  def dubboProviderHost = systemConfig.getString("dubbo-provider-host")

  def dubboProviderPort = systemConfig.getInt("dubbo-provider-port")

  def etcdHost = systemConfig.getString("etcd-host")

  def etcdPort = systemConfig.getInt("etcd-port")
}
