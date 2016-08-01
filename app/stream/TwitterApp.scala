package stream

import akka.actor.{ActorRef, ActorSystem, Props}
import twitter4j.{FilterQuery, _}
import org.apache.kafka.clients.producer.KafkaProducer


object TwitterApp extends App {

  val system = ActorSystem("TwitterActorSystem")

  val props = new java.util.Properties()
  props.put("bootstrap.servers", "***:9092")
  props.put("acks", "all");
  props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[Long, String](props)


  def simpleStatusListener = new StatusListener() {
    def onStatus(status: Status) {
      val twitActor: ActorRef = system.actorOf(Props(new TwitterActor(producer, "test")), "twitActor" + status.getId )
      twitActor ! TwitterDataPullRequest(status)
    }

    def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}

    def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}

    def onException(ex: Exception) {
      ex.printStackTrace
    }

    def onScrubGeo(arg0: Long, arg1: Long) {}

    def onStallWarning(warning: StallWarning) {}
  }

  println("printing  args")
  println(args(0))

  val twitterStream = new TwitterStreamFactory().getInstance
  twitterStream.addListener(simpleStatusListener)
  twitterStream.filter(new FilterQuery().track(Array("iyi")))

  twitterStream.sample()

  // producer.close()


}