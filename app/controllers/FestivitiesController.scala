package controllers

import javax.inject.{Inject, Singleton}

import play.api.libs.json.Json
import play.api.mvc._
import services.XMLDataLoader

@Singleton
class FestivitiesController @Inject()(xmlLoader: XMLDataLoader) extends Controller {
  def getAll = Action { Ok(xmlLoader.getAll().mkString(",")) }
}
