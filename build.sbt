
name := "TianChi4thCompetition"

version := "0.1"

scalaVersion := "2.12.5"
libraryDependencies ++= Settings.dependencies.value

// 集成测试目录
unmanagedSourceDirectories in Compile ++= Seq(baseDirectory(_ / "src" / "integration_test" / "scala").value)
unmanagedResourceDirectories in Compile ++= Seq(baseDirectory(_ / "src" / "integration_test" / "resources").value)
test in assembly := {} // ignore test

mainClass in assembly := {
  Some("cn.goldlokedu.alicomp.Boot")
}

// 打包
assemblyMergeStrategy in assembly := {
  case "consumer.conf" ⇒ MergeStrategy.concat
  case "reference.conf" ⇒ MergeStrategy.concat
  case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}
assemblyJarName in assembly := "mesh-agent.jar"
