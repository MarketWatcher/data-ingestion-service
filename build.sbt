name := "marketwatcher-data-stream"

version := "1.0"

scalaVersion := "2.11.8"
val kafkaVersion = "0.9.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies ++= Seq( "org.twitter4j" % "twitter4j-stream" % "3.0.3",
  "com.typesafe.akka" %% "akka-actor" % "2.4.0",
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.slf4j" % "slf4j-log4j12" % "1.7.21",
  "org.scalamock" %% "scalamock-scalatest-support" % "3.2.2" % "test" copy (isChanging = false),
  "com.sksamuel.kafka.embedded" % "embedded-kafka_2.11" % "0.21.0" copy(isChanging = false),
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test)

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
