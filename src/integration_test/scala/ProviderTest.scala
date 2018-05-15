import cn.goldlokedu.alicomp.AliComp
import com.typesafe.config.{Config, ConfigFactory}

object ProviderTest extends App with AliComp{
  override def config:Config = ConfigFactory.load("integration_huangwj.conf")
    .getConfig("provider-config")
    .withFallback(ConfigFactory.load())
  println("Provider ready....")
}
