package controllers

import java.time.Instant
import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import services.{Festivity, XMLDataLoader}

@Singleton
class FestivitiesController @Inject()(xmlLoader: XMLDataLoader) extends Controller {
  implicit val festivityWrites = new Writes[Festivity] {
    def writes(f: Festivity) = Json.obj(
      "id" -> f.id,
      "name" -> f.name,
      "place" -> f.place,
      "start" -> f.start,
      "end" -> f.end
    )
  }

  def getAll = Action { Ok(Json.toJson(xmlLoader.getAll())) }

  private def festivityFromRequest(jsValue: Option[JsValue]): Option[Festivity] = {
    jsValue.flatMap {json =>
      val name = (json \ "name").asOpt[String]
      val place = (json \ "place").asOpt[String]
      val start = (json \ "start").asOpt[String]
      val end = (json \ "end").asOpt[String]
      println(name)
      println(place)
      println(start)
      println(end)
      val f:Option[Festivity] = for (
        n <- name;
        p <- place;
        s <- start;
        e <- end;
        sdate = Instant.parse(s);
        edate = Instant.parse(e)
      ) yield Festivity.EMPTY.setName(n).setPlace(p).setStart(sdate).setEnd(edate)
      f
    }
  }

  def create = Action {request =>
    val result:Option[Result] = festivityFromRequest(request.body.asJson).map(f => Ok(Json.toJson(xmlLoader.save(f))))
    result.getOrElse(BadRequest("Incomplete json data"))
  }

  def update(id: Long) = Action {request =>
    val result:Option[Result] = festivityFromRequest(request.body.asJson).map(f => Ok(Json.toJson(xmlLoader.update(f.setId(id)))))
    result.getOrElse(BadRequest("Incomplete json data"))
  }

  def query(field: String, value: String) = Action {
    Ok(Json.toJson(xmlLoader.query(field, value)))
  }
}
