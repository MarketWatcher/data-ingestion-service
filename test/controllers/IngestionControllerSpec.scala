package controllers


import java.io.File
import java.util
import java.util.{Properties, UUID}

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
  val twitterConfiguration = new ConfigurationBuilder().setDebugEnabled(false)
    .setOAuthConsumerKey(sys.env("twitterConsumerKey"))
    .setOAuthConsumerSecret(sys.env("twitterConsumerSecret"))
    .setOAuthAccessToken(sys.env("twitterAccessToken"))
    .setOAuthAccessTokenSecret(sys.env("twitterAccessTokenSecret")).build()

  val alertPipeline: AlertPipeline = new AlertPipeline(twitterConfiguration, producer, ActorSystem.create())
  override lazy val ingestionController = new IngestionController(alertPipeline)
}

class FakeAppLoader(producer: KafkaProducer[String, String]) extends ApplicationLoader {
  override def load(context: Context): Application = {
    new FakeApplicationComponents(context, producer).application
  }
}

class IngestionControllerSpec extends FlatSpec with MockFactory with BeforeAndAfter with OneAppPerSuite {

  val requestPayload: String = """ {"id": "d19d5122-645d-11e6-8b77-86f30ca893d3", "name": "Alert1", "requiredCriteria": "iyi"} """
  var kafka: EmbeddedKafka = _
  var kafkaConfig: EmbeddedKafkaConfig = _
  var producer: KafkaProducer[String, String] = _
  var consumer: KafkaConsumer[String, String] = _

  before {
    Thread.sleep(10000)//one sad line. reason is; test still connects to twitter unfortunately, regarding async nature of tw api there has to be a buffer between each test cases
    consumer = createEmbeddedKafkaConsumer(kafkaConfig, "tweets")
  }

  after {
    consumer.close()
  }

  override implicit lazy val app: Application = {
    kafkaConfig = EmbeddedKafkaConfig()
    kafka = new EmbeddedKafka(kafkaConfig)
    producer = createEmbeddedKafkaProducer(kafkaConfig)
    kafka.start()

    val appLoader = new FakeAppLoader(producer)
    val context = ApplicationLoader.createContext(
      new Environment(new File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test)
    )
    kafka.start()
    appLoader.load(context)
  }

 "Data Ingestion" should "be triggered after alert creation" in {
    val fakeRequest = FakeRequest(POST, "/init").withHeaders("Content-Type" -> "application/json")
      .withBody(requestPayload)
    val result = route(fakeRequest).get
    val records = consumer.poll(15000)

    assert(!records.isEmpty)
  }

  "Kafka messages" should " contain alert id and tweet text" in {
    val fakeRequest = FakeRequest(POST, "/init").withHeaders("Content-Type" -> "application/json")
      .withBody(requestPayload)
    val result = route(fakeRequest).get
    val records = consumer.poll(15000)

  }

  "Data Ingestion" should "return error when alert id is not provided" in {
    val json: String = """ {"requiredCriteria": "iyi"} """
    val fakeRequest = FakeRequest(POST, "/init").withHeaders("Content-Type" -> "application/json")
      .withBody(json)
    val result = route(fakeRequest).get
    assert(BAD_REQUEST == status(result))
  }

  "Data Ingestion" should "return error when required criteria is not provided" in {
    val json: String = """ {"id": "d19d5122-645d-11e6-8b77-86f30ca893d3"} """
    val fakeRequest = FakeRequest(POST, "/init").withHeaders("Content-Type" -> "application/json")
      .withBody(json)
    val result = route(fakeRequest).get
    assert(BAD_REQUEST == status(result))
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
    consumerProps.put("logger.isDebugEnabled", "false")
    val consumer = new KafkaConsumer[String, String](consumerProps)
    consumer.subscribe(util.Arrays.asList(topicName))
    consumer
  }

}
