package stream

import controllers.Alert
import org.apache.kafka.clients.producer.KafkaProducer
import twitter4j._
import twitter4j.conf.Configuration

class AlertPipeline(twitterStream: TwitterStream, statusListenerFactory: StatusListenerFactory) {

  def push(alertName: String, alertRequiredCriteria: String) = {
    val listener = statusListenerFactory.createKafkaStatusListener(alertName)
    twitterStream.addListener(listener)
    twitterStream.filter(new FilterQuery().track(Array(alertRequiredCriteria)))
  }

}
