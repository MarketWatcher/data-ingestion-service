package loader

import akka.actor.ActorSystem
import com.sksamuel.kafka.embedded.EmbeddedKafkaConfig
import controllers.IngestionController
import play.api.ApplicationLoader.Context
import play.api._
import play.api.i18n._
import play.api.inject._
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import router.Routes
import service.KafkaProducerFactory

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

  lazy val router: Router = new Routes(httpErrorHandler, ingestionController)

  lazy val ingestionController = new IngestionController(ActorSystem.create(), KafkaProducerFactory.create())
}