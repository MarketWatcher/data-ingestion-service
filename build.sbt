name := "data-ingestion-service"

version := "1.0"

scalaVersion := "2.11.8"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-stream" % "3.0.3",
  "com.typesafe.play" % "play-ws_2.11" % "2.5.4",
  "org.apache.kafka" % "kafka-clients" % "0.8.2.1",
  "org.apache.kafka" % "kafka-clients" % "0.9.0.0" % Test,
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test" copy (isChanging = false),
  "com.sksamuel.kafka.embedded" % "embedded-kafka_2.11" % "0.21.0" % Test copy(isChanging = false),
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
