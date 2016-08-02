package service

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import com.sksamuel.kafka.embedded.{EmbeddedKafka, EmbeddedKafkaConfig}
import javax.inject._
import java.util.{Properties}
@Singleton
class KafkaService {
    def createKafkaProducer() : KafkaProducer[Long, String] = {
        var kafkaEnv = ""
        try {
            kafkaEnv = sys.env("KAFKA_ENV")
        } catch {
            case e: Exception => println("exception caught: " + e);
        }

        if (kafkaEnv != "" && kafkaEnv == "TEST") {
            println("TEST")
            var kafkaConfig = EmbeddedKafkaConfig()
            return generateEmbeddedKafkaProducer(kafkaConfig)
        } else {
            println("PROD")
            val props = new java.util.Properties()
            props.put("bootstrap.servers", "***:9092")
            props.put("acks", "all");
            props.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
            props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
            return new KafkaProducer[Long, String](props)
        }
    }

    def generateEmbeddedKafkaProducer(conf: EmbeddedKafkaConfig): KafkaProducer[Long, String] = {
        val producerProps = new Properties
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "***:9092")
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.LongSerializer")
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    new KafkaProducer[Long, String](producerProps)
  }

}
