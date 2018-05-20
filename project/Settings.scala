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
    val akkaVersion = "2.5.12"
    val akkaHttpVersion = "10.1.0"
    val twitterChillAkka = "0.9.2"
    val scalatestVersion = "3.0.3"
    val log4j2Version = "2.9.0"
    val slf4jVersion = "1.7.25"
    val etcdClientVersion = "0.1.4"
  }

  val dependencies = Def.setting(Seq(
    "com.typesafe.akka" %% "akka-actor" % versions.akkaVersion,
    "com.typesafe.akka" %% "akka-http" % versions.akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % versions.akkaHttpVersion,
    "com.typesafe.akka" %% "akka-remote" % versions.akkaVersion,
    "com.github.mingchuno" %% "etcd4s-core" % versions.etcdClientVersion,
    "com.twitter" %% "chill-akka" % versions.twitterChillAkka,

  // log
    "com.typesafe.akka" %% "akka-slf4j" % versions.akkaVersion,
    "org.slf4j" % "slf4j-api" % versions.slf4jVersion,
    "org.apache.logging.log4j" % "log4j-slf4j-impl" % versions.log4j2Version,
    "org.apache.logging.log4j" % "log4j-api" % versions.log4j2Version,
    "org.apache.logging.log4j" % "log4j-core" % versions.log4j2Version
//    "org.apache.commons" % "commons-lang3" % "3.7",

    // test
//    "com.typesafe.akka" %% "akka-multi-node-testkit" % versions.akkaVersion % Test,
//    "com.typesafe.akka" %% "akka-testkit" % versions.akkaVersion % Test,
//    "com.typesafe.akka" %% "akka-stream-testkit" % versions.akkaVersion % Test,
//    "com.typesafe.akka" %% "akka-http-testkit" % versions.akkaHttpVersion % Test,
//    "org.scalatest" %% "scalatest" % versions.scalatestVersion % Test
  ))


}
