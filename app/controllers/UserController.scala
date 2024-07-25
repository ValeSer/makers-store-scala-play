package controllers

import javax.inject._
import play.api.mvc._
import daos.UserDAO
import models.User
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
import org.mindrot.jbcrypt.BCrypt

@Singleton
class UserController @Inject()(cc: ControllerComponents, userDAO: UserDAO)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  val userForm: Form[User] = Form(
    mapping(
      "id" -> optional(longNumber),
      "username" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText
    )(User.apply)(User.unapply)
  )

  def showSignUpForm = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.signup(""))
  }

  def signUp = Action.async(parse.json) { implicit request =>
    val json = request.body.as[JsObject]
    val username = (json \ "username").as[String]
    val email = (json \ "email").as[String]
    val password = (json \ "password").as[String]

    val user = User(None, username, email, password)
    if (user.isValid) {
      userDAO.addUser(user).map { id =>
        Created(Json.obj("status" -> "success", "message" -> s"User $id created"))
      }.recover {
        case _ => InternalServerError(Json.obj("status" -> "error", "message" -> "User could not be created"))
      }
    } else {
      Future.successful(BadRequest("Invalid data"))
    }
  }

  def logIn = Action.async(parse.json) { implicit request =>
    (request.body \ "username").asOpt[String].zip((request.body \ "password").asOpt[String]).map {
      case (username, password) =>
        userDAO.findUserByUsername(username).map {
          case Some(user) if BCrypt.checkpw(password, user.password) =>
            val sessionId = java.util.UUID.randomUUID().toString
            DummySessionStorage.store(sessionId, username)
            Ok(Json.obj("status" -> "success", "message" -> "Logged in")).withSession("sessionId" -> sessionId)
          case _ => Unauthorized(Json.obj("status" -> "error", "message" -> "Invalid credentials"))
        }
    }.getOrElse(Future.successful(BadRequest("Invalid login data")))
  }

  object DummySessionStorage {
    private var sessions: Map[String, String] = Map()

    def store(sessionId: String, username: String): Unit = {
      sessions += (sessionId -> username)
    }

    def remove(sessionId: String): Unit = {
      sessions -= sessionId
    }

    def exists(sessionId: String): Boolean = {
      sessions.contains(sessionId)
    }

    def getUsername(sessionId: String): Option[String] = {
      sessions.get(sessionId)
    }
  }
 }
