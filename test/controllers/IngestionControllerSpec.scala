package controllers


import java.io.File
import java.util
import java.util.Properties

import akka.actor.ActorSystem
import com.sksamuel.kafka.embedded.{EmbeddedKafka, EmbeddedKafkaConfig}
import loader.Components
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.ApplicationLoader.Context
import play.api._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import stream.AlertPipeline
import twitter4j.conf.ConfigurationBuilder

class FakeApplicationComponents(context: Context, producer: KafkaProducer[String, String]) extends Components(context) {
  val twitterConfiguration = new ConfigurationBuilder().setDebugEnabled(true)
    .setOAuthConsumerKey(sys.env("twitterConsumerKey"))
    .setOAuthConsumerSecret(sys.env("twitterConsumerSecret"))
    .setOAuthAccessToken(sys.env("twitterAccessToken"))
    .setOAuthAccessTokenSecret(sys.env("twitterAccessTokenSecret")).build()

  private val alertPipeline: AlertPipeline = new AlertPipeline(twitterConfiguration, producer, ActorSystem.create())
  override lazy val ingestionController = new IngestionController(alertPipeline)
}

class FakeAppLoader(producer: KafkaProducer[String, String]) extends ApplicationLoader {
  override def load(context: Context): Application = {
    new FakeApplicationComponents(context, producer).application
  }
}

class IngestionControllerSpec extends FlatSpec with MockFactory with BeforeAndAfter with OneAppPerSuite {

  var kafka: EmbeddedKafka = null
  var kafkaConfig: EmbeddedKafkaConfig = null
  var producer: KafkaProducer[String, String] = null

  after {
    producer.close()
    kafka.stop()
  }

  override implicit lazy val app: Application = {
    kafkaConfig = EmbeddedKafkaConfig()
    kafka = new EmbeddedKafka(kafkaConfig)
    producer = createEmbeddedKafkaProducer(kafkaConfig)

    val appLoader = new FakeAppLoader(producer)
    val context = ApplicationLoader.createContext(
      new Environment(new File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test)
    )
    appLoader.load(context)
  }

  "Data Ingestion" should "be triggered after alert creation" in {

    val alertName: String = "Alert1"
    kafka.start()
    val consumer = createEmbeddedKafkaConsumer(kafkaConfig, alertName)

    val json: String = """ {"id": 1, "name": "Alert1", "requiredCriteria": "iyi"} """
    val fakeRequest = FakeRequest(POST, "/init").withHeaders("Content-Type" -> "application/json")
      .withBody(json)
    val result = route(fakeRequest).get
    val records = consumer.poll(30000)

    consumer.close()

    val iterator: util.Iterator[ConsumerRecord[String, String]] = records.iterator
    assert(iterator.hasNext)
  }

  def createEmbeddedKafkaProducer(config : EmbeddedKafkaConfig) : KafkaProducer[String, String] = {
    val producerProps = new Properties
    producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "***:" + config.kafkaPort)
    producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    new KafkaProducer[String, String](producerProps)
  }

  def createEmbeddedKafkaConsumer(conf: EmbeddedKafkaConfig, topicName: String): KafkaConsumer[String, String] = {
    val consumerProps = new Properties
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "***:" + kafkaConfig.kafkaPort)
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "myconsumer")
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[String, String](consumerProps)
    consumer.subscribe(util.Arrays.asList(topicName))
    consumer
  }

}
