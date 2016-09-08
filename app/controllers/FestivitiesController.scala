package controllers

import java.time.Instant
import javax.inject.{Inject, Singleton}

import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc._
import services.{Festivity, DataAccess}

@Singleton
class FestivitiesController @Inject()(dataAccess: DataAccess) extends Controller {
  implicit val festivityWrites = new Writes[Festivity] {
    def writes(f: Festivity) = Json.obj(
      "id" -> f.id,
      "name" -> f.name,
      "place" -> f.place,
      "start" -> f.start,
      "end" -> f.end
    )
  }

  def getAll = Action { Ok(Json.toJson(dataAccess.getAll())) }

  private def festivityFromRequest(jsValue: Option[JsValue]): Option[Festivity] = {
    jsValue.flatMap {json =>
      val name = (json \ "name").asOpt[String]
      val place = (json \ "place").asOpt[String]
      val start = (json \ "start").asOpt[String]
      val end = (json \ "end").asOpt[String]
      for (
        n <- name;
        p <- place;
        s <- start;
        e <- end;
        sdate = Instant.parse(s);
        edate = Instant.parse(e)
      ) yield Festivity.EMPTY.setName(n).setPlace(p).setStart(sdate).setEnd(edate)
    }
  }

  private def validate(f: Festivity, fun: Festivity => Result): Result = {
    if(f.start.isBefore(f.end) || f.start == f.end) fun(f)
    else BadRequest("Start date can't be greater than End date")
  }

  def create = Action {request =>
    val result: Option[Result] = festivityFromRequest(request.body.asJson)
      .map(validate(_,(f) => Ok(Json.toJson(dataAccess.save(f)))))
    result.getOrElse(BadRequest("Incomplete data"))
  }

  def update(id: Long) = Action {request =>
    val result: Option[Result] = festivityFromRequest(request.body.asJson)
      .map(validate(_, (f) => Ok(Json.toJson(dataAccess.update(f.setId(id))))))
    result.getOrElse(BadRequest("Incomplete data"))
  }

  def query(field: String, value: String) = Action {
      Ok(Json.toJson(dataAccess.query(field, value)))
  }

  def query(field: String, value: Instant) = Action {
      Ok(Json.toJson(dataAccess.query(field, value)))
  }

  def queryTime(field: String, value: String) = Action {
    Ok(Json.toJson(dataAccess.query(field, Instant.parse(value))))
  }

  def between(start: String, end: String) = Action {
    Ok(Json.toJson(dataAccess.queryDateRange(Instant.parse(start), Instant.parse(end))))
  }
}
