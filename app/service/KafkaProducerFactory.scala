package service

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import com.sksamuel.kafka.embedded.{EmbeddedKafka, EmbeddedKafkaConfig}
import javax.inject._
import java.util.Properties

import org.apache.kafka.common.serialization.StringSerializer
object KafkaProducerFactory {
  def create() : KafkaProducer[Long, String] = {
    val props = new java.util.Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "***:9092")
    props.put("acks", "all")
    props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[Long, String](props)
  }

  def createEmbedded(config : EmbeddedKafkaConfig) : KafkaProducer[Long, String] = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "***:" + config.kafkaPort)
    producerProps.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
    producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[Long, String](producerProps)
  }

}
