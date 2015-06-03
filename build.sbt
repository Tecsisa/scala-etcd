name := "scala-etcd"

description := "A Scala client for etcd"

organization := "com.tecsisa"

version := "0.0.1"

scalaVersion := "2.11.6"

scalacOptions ++= Seq("-unchecked", "-deprecation","-target:jvm-1.7", "-encoding", "utf8", "-feature")

javacOptions += "-g:none"

publishMavenStyle := false

bintrayRepository := "maven-bintray-repo"

licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html"))

bintrayOrganization := Some("tecsisa")

libraryDependencies ++= {
  Seq(
    "net.databinder.dispatch" %% "dispatch-core" % "0.11.2",
    "org.json4s" %% "json4s-native" % "3.2.10",
    "ch.qos.logback"  % "logback-classic"  %  "1.1.1" % "test",
    "org.scalatest" %% "scalatest" % "2.2.1" % "test"
  )
}