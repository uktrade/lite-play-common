name := """zzz-common"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "redis.clients" % "jedis" % "2.8.1",
  javaWs,
  "org.assertj" % "assertj-core" % "3.4.1"
)

lazy val `zzz-common` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.7"