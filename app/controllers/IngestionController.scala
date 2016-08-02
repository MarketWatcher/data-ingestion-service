package controllers

import java.util.{Date, UUID}
import javax.inject._

import twitter4j.conf.ConfigurationBuilder
import play.api._
import play.api.mvc._
import play.api.libs.json._
import akka.actor.{ActorRef, ActorSystem, Props}
import twitter4j.{FilterQuery, _}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import stream.TwitterActor
import stream.TwitterDataPullRequest
import service.KafkaService

case class Alert(id: Int, name: String)

@Singleton
class IngestionController @Inject()(system: ActorSystem, kafkaService: KafkaService) extends Controller {
  case class Alert(
                      name: String,
                      requiredCriteria: String
                    )

  implicit val alertReads = Json.reads[Alert]

  def initIngestion() = Action(BodyParsers.parse.json) { request =>
    val producer = generateProducer()
    val alertResult = request.body.validate[Alert]
    alertResult.fold(
      errors => {
        BadRequest(Json.obj("status" ->"KO", "message" -> JsError.toJson(errors)))
      },
      alert => {
        connectToTwitter(alert.name, alert.requiredCriteria, producer)
      }
    )
  }

  def generateProducer() : KafkaProducer[Long, String] = {
      return kafkaService.createKafkaProducer()
  }

  def connectToTwitter(name: String, requiredCriteria: String, producer: KafkaProducer[Long, String]) = {

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

    var cb = new ConfigurationBuilder();
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey(sys.env("twitterConsumerKey"))
      .setOAuthConsumerSecret(sys.env("twitterConsumerSecret"))
      .setOAuthAccessToken(sys.env("twitterAccessToken"))
      .setOAuthAccessTokenSecret(sys.env("twitterAccessTokenSecret"))
    val twitterStream = new TwitterStreamFactory(cb.build()).getInstance
    twitterStream.addListener(simpleStatusListener)
    twitterStream.filter(new FilterQuery().track(Array(requiredCriteria)))
    println("SUCCESS")
    Ok("SUCCESS")
  }

}
