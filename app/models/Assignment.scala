package models

import play.api.db.DB
import anorm._
import play.api.Play.current
import anorm.SqlParser._
import play.Logger
import models.helper.RangerHelper.rangerQuery

case class Assignment(id: Long, timestamp: Long, title: String, content: String, publisher: String, icon_url: String, class_id: Int)

object Assignment {

  def findById(id: Long) = DB.withConnection {
    implicit c =>
      SQL("select * from assignment where uid={id}").on('id -> id).as(simple singleOpt)
  }

  def update(kg: Long, assignmentId: Long, assignment: Assignment) = DB.withConnection {
    implicit c =>
      val time = System.currentTimeMillis()
      SQL("update assignment set `timestamp`={time}, title={title}, content={content}, " +
        "publisher={publisher}, image={url}, class_id={class_id}" +
        " where school_id={kg} and uid={id}")
        .on(
          'kg -> kg.toString,
          'id -> assignmentId,
          'time -> time,
          'title -> assignment.title,
          'content -> assignment.content,
          'url -> assignment.icon_url,
          'publisher -> assignment.publisher,
          'class_id -> assignment.class_id,
          'kg -> kg.toString
        ).executeUpdate()

      findById(assignmentId)
  }


  val simple = {
    get[Long]("uid") ~
      get[Long]("timestamp") ~
      get[String]("title") ~
      get[String]("content") ~
      get[String]("publisher") ~
      get[String]("image") ~
      get[Int]("class_id") map {
      case id ~ t ~ title ~ content ~ publisher ~ image ~ classId =>
        Assignment(id, t, title, content, publisher, image, classId)
    }
  }

  def convertToArray(classes: Option[String]) = {
    classes match {
      case None => Seq()
      case Some(ids) => ids.split(",").map(_.toInt).toSeq
    }
  }

  def index(kg: Long, classId: Option[String], from: Option[Long], to: Option[Long], most: Option[Int]) = DB.withConnection {
    implicit c =>
      classId match {
        case Some(ids) =>
          SQL("select * from assignment where school_id={kg} and class_id in (%s) ".format(ids) + rangerQuery(from, to))
            .on(
              'kg -> kg.toString,
              'from -> from,
              'to -> to
            ).as(simple *)
        case None =>
          SQL("select * from assignment where school_id={kg} " + rangerQuery(from, to))
            .on(
              'kg -> kg.toString,
              'from -> from,
              'to -> to
            ).as(simple *)
      }

  }
}