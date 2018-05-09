import cn.goldlokedu.alicomp.AliComp
import com.typesafe.config.{Config, ConfigFactory}

object ConsumerAgentTest extends App with AliComp {
  override def config: Config = ConfigFactory.load("integration_huangwj.conf")
    .getConfig("consumer-config")
    .withFallback(ConfigFactory.load())
  println("Consumer ready....")
}
