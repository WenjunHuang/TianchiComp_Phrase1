import sbt._

object Settings {
  val scalacOptions = Seq(
    "-language:_",
    "-unchecked",
    "-deprecation",
    "feature",
    "-opt:_"
  )

  object versions {
    val scalatestVersion = "3.0.3"
    val log4j2Version = "2.9.0"
    val slf4jVersion = "1.7.25"
    val etcdClientVersion = "0.1.4"
    val nettyVersion = "4.1.24.Final"
  }

  val dependencies = Def.setting(Seq(
    "io.netty" % "netty-all" % versions.nettyVersion,
    "com.typesafe" % "config" % "1.3.3",
    "io.spray" %% "spray-json" % "1.3.3",
    "com.github.mingchuno" %% "etcd4s-core" % versions.etcdClientVersion excludeAll (
      ExclusionRule(organization = "io.netty")
      ),

    // log
    "org.slf4j" % "slf4j-api" % versions.slf4jVersion,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % versions.log4j2Version,
    "org.apache.logging.log4j" % "log4j-api" % versions.log4j2Version,
    "org.apache.logging.log4j" % "log4j-core" % versions.log4j2Version
  ))


}
