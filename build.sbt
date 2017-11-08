name := """zzz-common"""

version := "1.0-SNAPSHOT"

// Disable Scaladoc
publishArtifact in(Compile, packageDoc) := false

libraryDependencies ++= Seq(
  "redis.clients" % "jedis" % "2.9.0",
  guice,
  "redis.clients" % "jedis" % "2.9.0",
  cacheApi,
  javaWs,
  filters,
  "io.mikael" % "urlbuilder" % "2.0.7",
  "org.mockito" % "mockito-all" % "1.10.19",
  "org.pac4j" % "pac4j" % "2.2.0",
  "org.pac4j" % "pac4j-saml" % "2.2.0",
  "org.pac4j" %% "play-pac4j" % "5.0.0",
  "org.bitbucket.b_c" % "jose4j" % "0.6.1",
  "com.github.tomakehurst" % "wiremock" % "2.9.0",
  "commons-io" % "commons-io" % "2.6",
  "io.pivotal.labs" % "cf-env" % "0.0.1",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.269",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.269",
  "com.spotify" % "completable-futures" % "0.3.2",
  "org.apache.commons" % "commons-collections4" % "4.1",
  "org.glassfish" % "javax.el" % "3.0.1-b08",
  "com.github.karelcemus" %% "play-redis" % "2.0.1",
  "org.assertj" % "assertj-core" % "3.5.2" % "test",
  "au.com.dius" % "pact-jvm-consumer-junit_2.11" % "3.5.8" % "test"
)

lazy val `zzz-common` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.8"

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo