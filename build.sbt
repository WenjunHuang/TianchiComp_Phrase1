
name := "TianChi4thCompetition"

version := "0.1"

scalaVersion := "2.12.5"
libraryDependencies ++= Settings.dependencies.value

// 集成测试目录
unmanagedSourceDirectories in Compile ++= Seq(baseDirectory(_ / "src" / "integration_test" / "scala").value)
unmanagedResourceDirectories in Compile ++= Seq(baseDirectory(_ / "src" / "integration_test" / "resources").value)
