name := "scala-etcd"

organization := "com.tecsisa"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-unchecked", "-deprecation","-target:jvm-1.7", "-encoding", "utf8", "-feature")

javacOptions += "-g:none"

libraryDependencies ++= {
  Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "org.json4s" %% "json4s-native" % "3.2.10",
    "ch.qos.logback"  % "logback-classic"  %  "1.1.1" % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  )
}