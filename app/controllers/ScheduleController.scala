package controllers

import play.api.mvc._
import play.api.libs.json.{JsError, Json}
import models._
import models.DaySchedule
import models.SchedulePreviewResponse
import models.WeekSchedule

object ScheduleController extends Controller {
  implicit val write1 = Json.writes[SchedulePreviewResponse]
  implicit val write2 = Json.writes[DaySchedule]
  implicit val write3 = Json.writes[WeekSchedule]
  implicit val write4 = Json.writes[ScheduleDetail]

  implicit val read1 = Json.reads[DaySchedule]
  implicit val read2 = Json.reads[WeekSchedule]
  implicit val read3 = Json.reads[ScheduleDetail]
  implicit val read4 = Json.reads[Schedule]

  case class EmptyResult(error_code: Int)

  implicit val write5 = Json.writes[EmptyResult]

  def preview(kg: Long, classId: Long) = Action {
    Schedule.preview(kg, classId) match {
      case Nil => Ok(Json.toJson(EmptyResult(1)))
      case List(r) => Ok(Json.toJson(List(r)))
    }
  }

  def show(kg: Long, classId: Long, scheduleId: Long) = Action {
    Schedule.show(kg, classId, scheduleId) match {
      case Some(r) => Ok(Json.toJson(r))
      case _ => Ok(Json.toJson(EmptyResult(1)))
    }

  }

  def update(kg: Long, classId: Long, scheduleId: Long) = Action(parse.json) {
    implicit request =>
      request.body.validate[ScheduleDetail].map {
        case (detail) =>
          Ok(Json.toJson(Schedule.insertNew(detail)))
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }

  def index(kg: Long, classId: Long) = Action {
    Ok(Json.toJson(Schedule.all(kg, classId)))
  }

  def create(kg: Long, classId: Long) = Action(parse.json) {
    implicit request =>
      request.body.validate[Schedule].map {
        case (s) =>
          Ok(Json.toJson(Schedule.create(kg, classId, s)))
      }.recoverTotal {
        e => BadRequest("Detected error:" + JsError.toFlatJson(e))
      }
  }
}
