package loader

import akka.actor.ActorSystem
import controllers.IngestionController
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n._
import play.api.inject._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import router.Routes
import service.KafkaProducerFactory
import stream.AlertPipeline
import twitter4j.conf.ConfigurationBuilder

class Loader extends ApplicationLoader {
  override def load(context: Context): Application = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new Components(context).application
  }
}

class Components(context: ApplicationLoader.Context)
  extends BuiltInComponentsFromContext(context)
    with I18nComponents
    with AhcWSComponents {

  override lazy val injector =  {
    new SimpleInjector(NewInstanceInjector) +
      router +
      cookieSigner +
      csrfTokenSigner +
      httpConfiguration +
      tempFileCreator +
      global +
      crypto +
      wsApi +
      messagesApi
  }

  private def buildAlertPipeline(): AlertPipeline = {

    val twitterConfigurationBuilder = new ConfigurationBuilder()

    twitterConfigurationBuilder.setDebugEnabled(false)
      .setOAuthConsumerKey(sys.env("twitterConsumerKey"))
      .setOAuthConsumerSecret(sys.env("twitterConsumerSecret"))
      .setOAuthAccessToken(sys.env("twitterAccessToken"))
      .setOAuthAccessTokenSecret(sys.env("twitterAccessTokenSecret"))

    val kafkaConnectionString = sys.env("KAFKA_CONNECTION_STRING")

    lazy val kafkaProducer = KafkaProducerFactory.create(kafkaConnectionString)
    lazy val actorSystem = ActorSystem.create()

    new AlertPipeline(twitterConfigurationBuilder.build(), kafkaProducer, actorSystem)

  }

  lazy val router: Router = new Routes(httpErrorHandler, ingestionController)
  lazy val ingestionController = new IngestionController(buildAlertPipeline())
}