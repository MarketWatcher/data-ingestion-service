package service

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}

object KafkaProducerFactory {
  def create(kafkaConnectionString: String) : KafkaProducer[String, String] = {
    val props = new java.util.Properties()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnectionString)
    props.put(ProducerConfig.ACKS_CONFIG, "all")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[String, String](props)
  }
}
