name := """zzz-common"""

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  "redis.clients" % "jedis" % "2.8.1",
  cache,
  javaWs,
  filters,
  "org.assertj" % "assertj-core" % "3.4.1",
  "io.mikael" % "urlbuilder" % "2.0.7",
  "org.assertj" % "assertj-core" % "3.5.2" % "test",
  "org.mockito" % "mockito-all" % "1.10.19"
)

lazy val `zzz-common` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"