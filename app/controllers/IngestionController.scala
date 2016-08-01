package controllers

import java.util.{Date, UUID}
import javax.inject._

import play.api._
import play.api.mvc._
import play.api.libs.json._
import akka.actor.{ActorRef, ActorSystem, Props}
import twitter4j.{FilterQuery, _}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import stream.TwitterActor
import stream.TwitterDataPullRequest

case class Alert(id: Int, name: String)

@Singleton
class IngestionController @Inject()(system: ActorSystem) extends Controller {

  def index = Action {
    Ok("Hellooooo")
  }

  case class Alert(
                      name: String,
                      requiredCriteria: String
                    )

  implicit val alertReads = Json.reads[Alert]

  def initIngestion() = Action(BodyParsers.parse.json) { request =>
    val alertResult = request.body.validate[Alert]
    alertResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toJson(errors)))
      },
      alert => {
        connectToTwitter(alert.name, alert.requiredCriteria)
      }
    )
  }

  def connectToTwitter(name: String, requiredCriteria: String) = {
    val props = new java.util.Properties()
    props.put("bootstrap.servers", "***:9092")
    props.put("acks", "all");
    props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[Long, String](props)

    def simpleStatusListener = new StatusListener() {
      def onStatus(status: twitter4j.Status) {
        val twitActor: ActorRef = system.actorOf(Props(new TwitterActor(producer, name)), "twitActor" + status.getId)
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

    val twitterStream = new TwitterStreamFactory().getInstance
    twitterStream.addListener(simpleStatusListener)
    twitterStream.filter(new FilterQuery().track(Array(requiredCriteria)))
    Thread.sleep(50000)
    Ok("SUCCESS")
  }

}
