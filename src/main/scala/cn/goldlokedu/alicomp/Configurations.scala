package cn.goldlokedu.alicomp

import com.typesafe.config.Config

trait Configuration {
  def config: Config
}

// 系统配置参数
trait SystemConfiguration {
  this: Configuration ⇒
  private def systemConfig = config.getConfig("system")


  def etcdHost = systemConfig.getString("etcd-host")

  def etcdPort = systemConfig.getInt("etcd-port")

  def runType = systemConfig.getString("type")
}

trait ConsumerConfiguration {
  this: Configuration =>
  private def consumerConfig = config.getConfig("consumer")

  def consumerHttpHost = consumerConfig.getString("http-host")

  def consumerHttpPort = consumerConfig.getInt("http-port")

  def consumerAgentCount = consumerConfig.getInt("agent-count")
}

trait ProviderConfiguration {
  this: Configuration =>
  private def providerConfig = config.getConfig("provider")

  def providerHost = providerConfig.getString("host")

  def dubboProviderHost = providerConfig.getString("dubbo-provider-host")

  def dubboProviderPort = providerConfig.getInt("dubbo-provider-port")

  def dubboProviderConnectionCount = providerConfig.getInt("dubbo-provider-connection-count")

  def dubboProviderMaxConcurrentCountPerConnection = providerConfig.getInt("dubbo-provider-max-concurrent-count-per-connection")
}