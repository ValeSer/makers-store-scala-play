package models

import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import slick.lifted.{ProvenShape, Tag}

// User case class
case class User(id: Option[Long], username: String, email: String, password: String) {
  def isValid: Boolean = {
    username.nonEmpty
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
