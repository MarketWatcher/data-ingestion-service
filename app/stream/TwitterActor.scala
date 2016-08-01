package stream

import akka.actor.Actor
import twitter4j._
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

case class TwitterDataPullRequest(status: Status)

class TwitterActor (producer: KafkaProducer[Long,String], topicName: String) extends Actor {
  def receive = {
    case TwitterDataPullRequest(status) =>
      val record = new ProducerRecord[Long, String](topicName, status.getId, status.getText)

      try {
     		producer.send(record)
    	} catch {
        case e: Exception => 
          println(e.getMessage)
    	}

        //println(status)
  }
}