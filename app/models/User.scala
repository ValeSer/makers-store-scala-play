package models

import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}

import scala.util.matching.Regex

// User case class
case class User(id: Option[Long], username: String, email: String, password: String) {
  def isValid: Boolean = {
    val pattern = new Regex(".+@example\\.com$")
    (username.isEmpty, pattern.findFirstMatchIn(email), password.length < 8) match {
      case (true, _, _) => false
      case (_, None, _) => false
      case (_, _, true) => false
      case _ => true
    }
  }
}


// Companion object for User
object User {
  implicit val userFormat: OFormat[User] = Json.format[User]
}

// Users table definition
class Users(tag: Tag) extends Table[User](tag, "users") {
  def id: Rep[Long] = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def username: Rep[String] = column[String]("username")
  def email: Rep[String] = column[String]("email")
  def password: Rep[String] = column[String]("password")

  def * : ProvenShape[User] = (id.?, username, email, password) <> ((User.apply _).tupled, User.unapply)
}

// Companion object for Users table
object Users {
  val table = TableQuery[Users]
}
