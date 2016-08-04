package controllers

import javax.inject._

import play.api.libs.json._
import play.api.mvc._
import stream.AlertPipeline

case class Alert(id: Int, name: String, requiredCriteria: String)

@Singleton
class IngestionController (alertPipeline: AlertPipeline) extends Controller {

  implicit val alertReads = Json.reads[Alert]

  def initIngestion() = Action(BodyParsers.parse.json) { request =>
    val alertResult = request.body.validate[Alert]
    alertResult.fold(
      errors => {
        BadRequest(Json.obj("status" -> "KO", "message" -> JsError.toJson(errors)))
      },
      alert => {
        alertPipeline.push(alert)

        Ok("SUCCESS")
      }
    )
  }

}
