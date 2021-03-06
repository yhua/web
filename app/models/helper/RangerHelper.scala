package models.helper

object RangerHelper {
  def generateFrom(from: Option[Long], field: Option[String]) = {
    from map {
      f =>
        " and %s > {from}".format(field.getOrElse("uid"))
    }
  }

  def generateTo(to: Option[Long], field: Option[String]) = {
    to map {
      t =>
        " and %s < {to}".format(field.getOrElse("uid"))
    }
  }

  def rangerQuery(from: Option[Long], to: Option[Long]) = {
    rangerQueryWithField(from, to, None)
  }

  def rangerQueryWithField(from: Option[Long], to: Option[Long], field: Option[String]) = {
    " " + generateFrom(from, field).getOrElse("") + generateTo(to, field).getOrElse("") + " order by " + field.getOrElse("uid") + " DESC"
  }

}
