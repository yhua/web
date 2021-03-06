package models

import anorm.SqlParser._
import anorm._
import play.api.db.DB
import play.api.Play.current
import models.json_models.BindNumberResponse._
import anorm.~
import models.json_models.ChildInfo
import java.util.Date
import play.Logger
import models.helper.TimeHelper.any2DateTime

case class Parent(parent_id: Option[String], school_id: Long, name: String, phone: String, portrait: Option[String], gender: Int, birthday: String, timestamp: Option[Long])

case class ParentInfo(id: Option[Long], birthday: String, gender: Int, portrait: String, name: String, phone: String, kindergarten: School, relationship: String, child: ChildInfo, card: String)


object Parent {

  def allowToAccess(phone: String, token: Option[String], kg: Long) = DB.withConnection {
    implicit c =>
      Logger.info(token.toString)
      SQL("select count(1) from parentinfo p, accountinfo a where a.accountid=p.phone " +
        "and phone={phone} and school_id={kg} and a.pwd_change_time={token} and status=1")
        .on(
          'kg -> kg.toString,
          'phone -> phone,
          'token -> token
        ).as(get[Long]("count(1)") single) > 0
  }

  def canAccess(phoneNum: Option[String], token: Option[String], kg: Long)  = phoneNum.exists {
    case (phone) if allowToAccess(phone, token, kg) => true
    case _ => false
  }

  def existsInOtherSchool(parent: Parent) = DB.withConnection {
    implicit c =>
      SQL("select count(1) as count from parentinfo where phone={phone}")
        .on(
          'phone -> parent.phone
        ).as(get[Long]("count") single) > 0
  }

  def idExists(parentId: Option[String]): Boolean = DB.withConnection {
    implicit c =>
      parentId match {
        case Some(id) =>
          SQL("select count(1) as count from parentinfo where parent_id={parent_id}")
            .on('parent_id -> id)
            .as(get[Long]("count") single) > 0
        case None => false
      }
  }

  def phoneExists(kg: Long, phone: String): Boolean = DB.withConnection {
    implicit c =>
      SQL("select count(1) as count from parentinfo where phone={phone} and school_id={kg}")
        .on(
          'phone -> phone,
          'kg -> kg.toString
        ).as(get[Long]("count") single) > 0
  }

  def update2(parent: Parent) = DB.withConnection {
    implicit c =>
      val timestamp = System.currentTimeMillis
      SQL("update parentinfo set name={name}, " +
        "phone={phone}, gender={gender}, company={company}, " +
        "picurl={picurl}, birthday={birthday}, " +
        "update_at={timestamp} where parent_id={parent_id}")
        .on('name -> parent.name,
          'phone -> parent.phone,
          'gender -> parent.gender,
          'company -> "",
          'picurl -> parent.portrait.getOrElse(""),
          'birthday -> parent.birthday,
          'parent_id -> parent.parent_id,
          'timestamp -> timestamp).executeUpdate()
      info(parent.school_id, parent.parent_id.get)
  }

  def updateWithPhone(kg: Long, parent: Parent) = DB.withConnection {
    implicit c =>
      val timestamp = System.currentTimeMillis
      SQL("update parentinfo set name={name}, gender={gender}, company={company}, " +
        "picurl={picurl}, birthday={birthday}, " +
        "update_at={timestamp} where phone={phone} and school_id={kg}")
        .on('name -> parent.name,
          'phone -> parent.phone,
          'gender -> parent.gender,
          'company -> "",
          'picurl -> parent.portrait.getOrElse(""),
          'birthday -> parent.birthday,
          'timestamp -> timestamp,
          'kg -> kg.toString
        ).executeUpdate()
      findByPhone(parent.school_id)(parent.phone)
  }

  def findByPhone(kg: Long)(phone: String) = DB.withConnection {
    implicit c =>
      SQL(fullStructureSql + " and p.phone={phone}")
        .on('kg -> kg,
          'phone -> phone)
        .as(withRelationship.singleOpt)
  }

  def delete(kg: Long)(phone: String) = DB.withConnection {
    implicit c =>
      SQL("update parentinfo set status=0 where phone={phone}")
        .on('phone -> phone
        ).executeUpdate
  }

  @deprecated(message = "try to remove", since = "2014-03-12")
  def update(parent: ParentInfo) = DB.withTransaction {
    implicit c =>
      try {
        val child = parent.child
        val timestamp = System.currentTimeMillis
        SQL("update parentinfo set name={name}, " +
          "relationship={relationship}, phone={phone}, " +
          "gender={gender}, company={company}, picurl={picurl}, birthday={birthday}, " +
          "update_at={timestamp} where uid={uid}")
          .on('name -> parent.name,
            'relationship -> parent.relationship,
            'phone -> parent.phone,
            'gender -> parent.gender,
            'company -> "",
            'picurl -> parent.portrait,
            'birthday -> parent.birthday,
            'uid -> parent.id,
            'timestamp -> timestamp).executeUpdate()

        val (childId, parentId) = SQL("select child_id, p.parent_id from relationmap r, parentinfo p where p.parent_id = r.parent_id and p.uid={uid}")
          .on('uid -> parent.id
          ).as((get[String]("child_id") ~ get[String]("parent_id") map {
          case child ~ parent => (child, parent)
          case _ => ("", "")
        }) singleOpt).get
        Logger.info("child: %s, parent: %s".format(childId, parentId))
        SQL("update childinfo set name={name}, gender={gender}, " +
          "picurl={picurl}, birthday={birthday}, indate={indate}, " +
          "nick={nick}, update_at={timestamp}, class_id={class_id} " +
          " where child_id={child_id}")
          .on('name -> child.name,
            'gender -> child.gender,
            'picurl -> child.portrait,
            'birthday -> child.birthday,
            'indate -> child.birthday,
            'nick -> child.nick,
            'class_id -> child.class_id,
            'child_id -> childId.toString,
            'timestamp -> timestamp).executeUpdate()
        SQL("update cardinfo set cardnum={cardnum} " +
          "where userid={userid}")
          .on('cardnum -> parent.card,
            'userid -> parentId.toString).executeUpdate()
        c.commit
        findById(parent.kindergarten.school_id)(parent.id.get)
      }
      catch {
        case t: Throwable =>
          Logger.info("error %s".format(t.toString))
          c.rollback
          findById(parent.kindergarten.school_id)(-1)
      }

  }


  def findById(kg: Long)(id: Long) = DB.withConnection {
    implicit c =>
      SQL(fullStructureSql + " and p.uid={id}")
        .on('kg -> kg,
          'id -> id)
        .as(withRelationship.singleOpt)
  }

  def create(kg: Long, parent: Parent) = DB.withConnection {
    implicit c =>
      val timestamp = System.currentTimeMillis
      val parent_id = parent.parent_id.getOrElse("2_%d_%d".format(kg, timestamp))
      val createdId: Option[Long] = SQL("INSERT INTO parentinfo(name, parent_id, relationship, phone, gender, company, picurl, birthday, school_id, status, update_at) " +
        "VALUES ({name},{parent_id},{relationship},{phone},{gender},{company},{picurl},{birthday},{school_id},{status},{timestamp})")
        .on(
          'name -> parent.name,
          'parent_id -> parent_id,
          'relationship -> "",
          'phone -> parent.phone,
          'gender -> parent.gender,
          'company -> "",
          'picurl -> parent.portrait.getOrElse(""),
          'birthday -> parent.birthday,
          'school_id -> kg.toString,
          'status -> 1,
          'timestamp -> timestamp).executeInsert()
      Logger.info("created parent %s".format(createdId))
      val accountinfoUid = SQL("INSERT INTO accountinfo(accountid, password, pushid, active, pwd_change_time) " +
        "VALUES ({accountid},{password},'',0,0)")
        .on('accountid -> parent.phone,
          'password -> generateNewPassword(parent.phone)).executeInsert()
      Logger.info("created accountinfo %s".format(accountinfoUid))
      createdId map (id => info(parent.school_id, parent_id))

  }

  @deprecated(message = "try to remove", since = "2014-03-12")
  def fullCreate(kg: Long, parent: ParentInfo) = DB.withTransaction {
    implicit c =>
      val child = parent.child
      val timestamp = System.currentTimeMillis
      val parent_id = "2_%d".format(timestamp)
      try {
        val createdId: Option[Long] = SQL("INSERT INTO parentinfo(name, parent_id, relationship, phone, gender, company, picurl, birthday, school_id, status, update_at) " +
          "VALUES ({name},{parent_id},{relationship},{phone},{gender},{company},{picurl},{birthday},{school_id},{status},{timestamp})")
          .on('name -> parent.name,
            'parent_id -> parent_id,
            'relationship -> parent.relationship,
            'phone -> parent.phone,
            'gender -> parent.gender,
            'company -> "",
            'picurl -> parent.portrait,
            'birthday -> parent.birthday,
            'school_id -> parent.kindergarten.school_id,
            'status -> 1,
            'timestamp -> timestamp).executeInsert()
        Logger.info("created parent %s".format(createdId))
        val child_id = child.child_id.getOrElse("1_%d".format(timestamp))
        child.child_id match {
          case None =>
            val childUid = SQL("INSERT INTO childinfo(name, child_id, student_id, gender, classname, picurl, birthday, indate, school_id, address, stu_type, hukou, social_id, nick, status, update_at, class_id) " +
              "VALUES ({name},{child_id},{student_id},{gender},{classname},{picurl},{birthday},{indate},{school_id},{address},{stu_type},{hukou},{social_id},{nick},{status},{timestamp},{class_id})")
              .on('name -> child.name,
                'child_id -> child_id,
                'student_id -> "%d".format(timestamp).take(5),
                'gender -> child.gender,
                'classname -> "水果班",
                'picurl -> child.portrait,
                'birthday -> child.birthday,
                'indate -> child.birthday,
                'school_id -> parent.kindergarten.school_id,
                'address -> "address",
                'stu_type -> 2,
                'hukou -> 1,
                'social_id -> "social_id",
                'nick -> child.nick,
                'status -> 1,
                'class_id -> child.class_id,
                'timestamp -> timestamp).executeInsert()
            Logger.info("created childinfo %s".format(childUid))
          case id =>
            Logger.info("connect to exists child, child_id = %s".format(id))
        }
        val relationmapUid = SQL("INSERT INTO relationmap(child_id, parent_id) VALUES ({child_id},{parent_id})")
          .on('child_id -> child_id,
            'parent_id -> parent_id
          ).executeInsert()
        Logger.info("created relationmap %s".format(relationmapUid))
        val count = SQL("select count(1) from accountinfo where accountid={accountid}")
          .on('accountid -> parent.phone).as(get[Long]("count(1)") single)
        Logger.info("count in accountinfo %d".format(count))
        count match {
          case 0 =>
            val accountinfoUid = SQL("INSERT INTO accountinfo(accountid, password, pushid, active, pwd_change_time) " +
              "VALUES ({accountid},{password},'',0,0)")
              .on('accountid -> parent.phone,
                'password -> generateNewPassword(parent.phone)).executeInsert()
            Logger.info("created accountinfo %s".format(accountinfoUid))
          case _ => Logger.info("phone already exists in accountinfo %s".format(parent.phone))
        }
        val cardinfoUid = SQL("INSERT INTO cardinfo(cardnum, userid, expiredate) " +
          "VALUES ({cardnum}, {userid}, '2200-01-01')")
          .on('cardnum -> parent.card,
            'userid -> parent_id).executeInsert()
        Logger.info("created cardinfo %s".format(cardinfoUid))
        c.commit
        findById(kg)(createdId.getOrElse(-1))
      }
      catch {
        case t: Throwable =>
          Logger.info("error %s".format(t.toString))
          c.rollback
          findById(kg)(-1)
      }

  }

  val withRelationship = {
    get[Long]("parentinfo.uid") ~
      get[String]("school_id") ~
      get[String]("parentinfo.name") ~
      get[Date]("parentinfo.birthday") ~
      get[Int]("parentinfo.gender") ~
      get[Option[String]]("parentinfo.picurl") ~
      get[String]("schoolinfo.name") ~
      get[String]("parentinfo.school_id") ~
      get[String]("parentinfo.relationship") ~
      get[String]("childinfo.name") ~
      get[String]("nick") ~
      get[Date]("childinfo.birthday") ~
      get[Int]("childinfo.gender") ~
      get[Option[String]]("childinfo.picurl") ~
      get[String]("childinfo.child_id") ~
      get[Int]("class_id") ~
      get[String]("cardnum") ~
      get[String]("phone") ~
      get[String]("classinfo.class_name") ~
      get[Long]("childinfo.update_at") map {
      case id ~ k_id ~ name ~ birthday ~ gender ~ portrait ~
        schoolName ~ schoolId ~ relationship ~ childName ~
        nick ~ childBirthday ~ childGender ~ childPortrait ~ child_id ~ classId ~ card ~ phone ~ className ~ childTime =>
        new ParentInfo(Some(id), birthday.toDateOnly, gender.toInt, portrait.getOrElse(""), name, phone,
          new School(schoolId.toLong, schoolName), relationship,
          new ChildInfo(Some(child_id), childName, nick, childBirthday.toDateOnly, childGender.toInt,
            Some(childPortrait.getOrElse("")), classId, Some(className), Some(childTime), Some(schoolId.toLong)), card)
    }
  }


  def show(kg: Long, phone: String) = DB.withConnection {
    implicit c =>
      SQL(simpleSql + " and p.phone = {phone}")
        .on('kg -> kg,
          'phone -> phone)
        .as(simple singleOpt)
  }

  val fullStructureSql = "select p.*, s.name, c.*, card.cardnum, ci.class_name from parentinfo p, schoolinfo s, childinfo c, relationmap r, cardinfo card, classinfo ci " +
    "where p.school_id = s.school_id and s.school_id={kg} and p.status=1 and ci.class_id=c.class_id " +
    "and r.child_id = c.child_id and r.parent_id = p.parent_id and card.userid = p.parent_id"

  def all(kg: Long, classId: Option[Long]): List[ParentInfo] = DB.withConnection {
    implicit c =>
      classId match {
        case Some(id) =>
          Logger.info("id = " + id.toString)
          SQL(fullStructureSql + " and c.class_id={class_id}")
            .on('kg -> kg,
              'class_id -> id.toString)
            .as(withRelationship *)
        case None =>
          SQL(fullStructureSql)
            .on('kg -> kg)
            .as(withRelationship *)
      }

  }

  val simple = {
    get[String]("parent_id") ~
      get[String]("school_id") ~
      get[String]("name") ~
      get[String]("phone") ~
      get[Int]("gender") ~
      get[Option[String]]("picurl") ~
      get[Date]("birthday") ~
      get[Long]("update_at") map {
      case id ~ kg ~ name ~ phone ~ gender ~ portrait ~ birthday ~ t =>
        new Parent(Some(id), kg.toLong, name, phone, Some(portrait.getOrElse("")), gender, birthday.toDateOnly, Some(t))
    }
  }

  val simpleSql = "select * from parentinfo p where school_id={kg} and status=1 "

  def info(kg: Long, parentId: String) = DB.withConnection {
    implicit c =>
      SQL(simpleSql + " and parent_id={parent_id} ")
        .on('kg -> kg,
          'parent_id -> parentId)
        .as(simple singleOpt)
  }

  def simpleIndex(kg: Long) = DB.withConnection {
    implicit c =>
      SQL(simpleSql)
        .on('kg -> kg)
        .as(simple *)
  }

  def indexInClass(kg: Long, classId: Long) = DB.withConnection {
    implicit c =>
      SQL(fullStructureSql + " and c.class_id={class_id}")
        .on('kg -> kg,
          'class_id -> classId)
        .as(withRelationship *)
  }

}
