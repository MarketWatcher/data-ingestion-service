package stream

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import controllers.Alert
import org.apache.kafka.clients.producer.KafkaProducer
import twitter4j._
import twitter4j.conf.Configuration

import scala.util.Random

class AlertPipeline(twitterConfiguration: Configuration, producer: KafkaProducer[String, String], actorSystem: ActorSystem) {

  def push(alert: Alert) = {
    val listener = createStatusListener(actorSystem, producer, alertId = alert.id)

    val twitterStreamFactory: TwitterStreamFactory = new TwitterStreamFactory(twitterConfiguration)
    val twitterStream = twitterStreamFactory.getInstance
    twitterStream.addListener(listener)

    twitterStream.filter(new FilterQuery().track(Array(alert.requiredCriteria)))
  }

  private def createStatusListener(actorSystem: ActorSystem, producer: KafkaProducer[String, String], alertId: UUID): StatusListener = {
    new StatusListener() {
      def onStatus(status: twitter4j.Status) {
        var twitActor: ActorRef = null
           twitActor = actorSystem.actorOf(Props(new TwitterActor(producer)), "twitActor" + Random.nextInt())
        twitActor ! TwitterDataPullRequest(status)
      }

      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {}

      def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {}

      def onException(ex: Exception) {
        ex.printStackTrace()
      }

      def onScrubGeo(arg0: Long, arg1: Long) {}

      def onStallWarning(warning: StallWarning) {}
    }
  }
}