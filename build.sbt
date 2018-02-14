name := """zzz-common"""

version := "1.0-SNAPSHOT"

// Disable Scaladoc
publishArtifact in(Compile, packageDoc) := false

libraryDependencies ++= Seq(
  "redis.clients" % "jedis" % "2.9.0",
  cache,
  javaWs,
  filters,
  guice,
  "io.mikael" % "urlbuilder" % "2.0.7",
  "org.assertj" % "assertj-core" % "3.5.2" % "test",
  "org.mockito" % "mockito-all" % "1.10.19",
  "org.pac4j" % "pac4j" % "1.9.0",
  "org.pac4j" % "pac4j-saml" % "1.9.0",
  "org.pac4j" % "play-pac4j" % "2.4.0",
  "au.com.dius" % "pact-jvm-consumer-junit_2.12" % "3.5.8" % "test",
  "org.bitbucket.b_c" % "jose4j" % "0.6.1",
  "com.github.tomakehurst" % "wiremock" % "2.9.0",
  "commons-io" % "commons-io" % "2.6",
  "io.pivotal.labs" % "cf-env" % "0.0.1",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.269",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.269",
  "com.spotify" % "completable-futures" % "0.3.2"
)

TwirlKeys.templateImports += "play.twirl.api.HtmlFormat"

lazy val `zzz-common` = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.12.4"

resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
resolvers += Resolver.jcenterRepo