package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import models._
import views._
import play.Logger

object Auth extends Controller {

  // -- Authentication

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    ) verifying("Invalid username or password", result => result match {
      case (username, password) => Employee.authenticate(username, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action {
    implicit request =>
      Ok(html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action {
    implicit request =>
      loginForm.bindFromRequest.fold(
        formWithErrors => BadRequest(html.login(formWithErrors)),
        user => {
          val login = Employee.authenticate(user._1, user._2).get
          Logger.info(login.toString)
          if (login.login_name == "operator")
            Redirect("/operation").withSession("username" -> login.login_name, "phone" -> login.phone, "name" -> login.name, "id" -> login.id.getOrElse(""))
          else
            Redirect("/admin#/kindergarten/%d".format(login.school_id)).withSession("username" -> login.name, "phone" -> login.phone, "name" -> login.name, "id" -> login.id.getOrElse(""))
        }

      )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

}

/**
 * Provide security features
 */
trait Secured {

  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = {
    request.session.get("username")
  }

  private def checkSchool(request: RequestHeader) = {
    val user = request.session.get("username")
    val id = request.session.get("id")
    Logger.info(id.toString)
    val token = request.session.get("token")
    Logger.info(token.toString)
    val Pattern = "/kindergarten/(\\d+)/.+".r
    request.path match {
      case Pattern(c) if Employee.canAccess(id, c.toLong) => user
      case Pattern(c) if Parent.canAccess(user, token, c.toLong) => user
      case _ => None
    }

  }


  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  private def onNotLoggedIn(request: RequestHeader) = Results.Unauthorized

  // --

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) {
    user =>
      Action(request => f(user)(request))
  }

  def IsLoggedIn(f: => String => Request[AnyContent] => Result) = Security.Authenticated(checkSchool, onNotLoggedIn) {
    user =>
      Action(request => f(user)(request))
  }

  def IsOperator(f: => String => Request[AnyContent] => Result) = IsAuthenticated {
    user => request =>
      if (user == "operator") {
        f(user)(request)
      } else {
        Results.Redirect(routes.Auth.login)
      }
  }

}