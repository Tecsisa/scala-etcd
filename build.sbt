name := "scala-etcd"

organization := "com.tecsisa"

version := "0.0.1"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-unchecked", "-deprecation","-target:jvm-1.7", "-encoding", "utf8", "-feature")

javacOptions += "-g:none"

libraryDependencies ++= {
  Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "org.json4s" %% "json4s-native" % "3.2.10",
    "org.slf4j" % "slf4j-api" % "1.7.5" % "test",
    "org.slf4j" % "slf4j-simple" % "1.7.5" % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  )
}