package controllers


import java.util
import java.util.Properties

import akka.actor.ActorSystem
import com.sksamuel.kafka.embedded.{EmbeddedKafka, EmbeddedKafkaConfig}
import loader.Components
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord, KafkaConsumer}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, FlatSpec}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api._
import play.api.ApplicationLoader.Context
import play.api.test.{FakeHeaders, FakeRequest}
import play.api.test.Helpers._
import service.KafkaProducerFactory
import stream.StatusListenerFactory
import java.io.File
import java.util.concurrent.TimeUnit

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import stream.AlertPipeline
import twitter4j._
import twitter4j.conf.ConfigurationBuilder
import java.io.InputStream

class StubAlertPipeline(twitterStream: TwitterStream, statusListenerFactory: StatusListenerFactory) extends AlertPipeline(twitterStream: TwitterStream, statusListenerFactory: StatusListenerFactory) {
    override def push(alertName: String, alertRequiredCriteria: String) = {
     produceSingleTweet(alertName, statusListenerFactory.producer)
    }

    def produceSingleTweet(alertName: String, producer: KafkaProducer[Long, String]) {
        val record = new ProducerRecord[Long, String](alertName, 1, "iyi iyi cok iyi")
        producer.send(record)
    }
}

class FakeApplicationComponents(context: Context, alertPipelineStub: AlertPipeline) extends Components(context)  {
  override lazy val ingestionController = new IngestionController(alertPipelineStub)
}


class FakeAppLoader(alertPipelineStub: AlertPipeline) extends ApplicationLoader {
  override def load(context: Context): Application = {
    new FakeApplicationComponents(context, alertPipelineStub).application
  }
}

class IngestionControllerSpec extends FlatSpec with MockFactory with BeforeAndAfter with OneAppPerSuite {

  var kafka: EmbeddedKafka = null
  var kafkaConfig: EmbeddedKafkaConfig = null
  var producer: KafkaProducer[Long, String] = null
  var statusStream: StatusStream

  before {
  }

  after {
    producer.close()
    kafka.stop()
  }

  override implicit lazy val app: Application = {
    kafkaConfig = EmbeddedKafkaConfig()
    kafka = new EmbeddedKafka(kafkaConfig)
    producer = KafkaProducerFactory.createEmbedded(kafkaConfig)

    val twitterConfiguration = new ConfigurationBuilder().setDebugEnabled(false)
      .setOAuthConsumerKey(sys.env("twitterConsumerKey"))
      .setOAuthConsumerSecret(sys.env("twitterConsumerSecret"))
      .setOAuthAccessToken(sys.env("twitterAccessToken"))
      .setOAuthAccessTokenSecret(sys.env("twitterAccessTokenSecret")).build()

    lazy val twitterStreamFactory: TwitterStreamFactory = new TwitterStreamFactory(twitterConfiguration)
    lazy val actorSystem = ActorSystem.create()
    lazy val statusListenerFactory = new StatusListenerFactory(producer, actorSystem)
    val twitterStream = twitterStreamFactory.getInstance

    //lazy val alertPipelineStub = mock[AlertPipeline]
    //alertPipelineStub.push(_: String, _: String).expects(*, *).onCall { alertName: String => produceSingleTweet(alertName, producer) }
    val alertPipelineStub: StubAlertPipeline = new StubAlertPipeline(twitterStream, statusListenerFactory)

    val appLoader = new FakeAppLoader(alertPipelineStub)
    val context = ApplicationLoader.createContext(
      new Environment(new File("."), ApplicationLoader.getClass.getClassLoader, Mode.Test)
    )
    appLoader.load(context)
  }

  "Data Ingestion" should "be triggered after alert creation" in {

    val alertName: String = "Alert1"
    kafka.start()
    val consumer = generateKafkaConsumer(kafkaConfig, alertName)

    val json: String = """ {"id": 1, "name": "Alert1", "requiredCriteria": "iyi"} """
    val fakeRequest = FakeRequest(POST, "/init").withHeaders("Content-Type" -> "application/json")
      .withBody(json)
    val result = route(fakeRequest).get
    println(status(result))

    val records = consumer.poll(30000)

    consumer.close()

    val iterator: util.Iterator[ConsumerRecord[Long, String]] = records.iterator
    assert(iterator.hasNext)
  }

  def generateKafkaConsumer(conf: EmbeddedKafkaConfig, topicName: String): KafkaConsumer[Long, String] = {
    val consumerProps = new Properties
    consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "***:" + kafkaConfig.kafkaPort)
    consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "myconsumer")
    consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.LongDeserializer")
    consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
    val consumer = new KafkaConsumer[Long, String](consumerProps)
    consumer.subscribe(util.Arrays.asList(topicName))
    consumer
  }

}
