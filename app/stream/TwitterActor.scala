package stream

import java.util.UUID

import akka.actor.Actor
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import twitter4j._

case class TwitterDataPullRequest(status: Status, alertId: UUID)

class TwitterActor (producer: KafkaProducer[String,String]) extends Actor {

  val topicName = "tweets"

  def receive = {
    case TwitterDataPullRequest(status: Status, alertId: UUID) =>
      val record = new ProducerRecord[String, String](topicName, alertId.toString, status.getText)
      try {
     		producer.send(record)
    	} catch {
        case e: Exception =>
          println(e.getMessage)
    	}
  }
}
