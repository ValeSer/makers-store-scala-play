package controllers

import daos.UserDAO
import models.Users
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.Play.materializer
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.CSRFTokenHelper.CSRFRequest
import play.api.test.Helpers._
import play.api.test._
import slick.jdbc.JdbcProfile
import slick.lifted
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext

class UserControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting {

  "UserController POST /signUp" should {

    "create a new user" in {
      val userDAO = inject[UserDAO]
      val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

      val request = FakeRequest(POST, "/signUp")
        .withJsonBody(Json.obj(
          "username" -> "testuser",
          "email" -> "test@example.com",
          "password" -> "password")
        )
        .withCSRFToken

      val result = call(userController.signUp, request)

      status(result) mustBe CREATED
      val jsonResponse = contentAsJson(result)
      (jsonResponse \ "status").as[String] mustBe "success"
      (jsonResponse \ "message").as[String] must include("User")

      // Verify user is actually created in the database
      val maybeUser = await(userDAO.findUserByUsername("testuser"))
      maybeUser must not be empty
      maybeUser.get.email mustBe "test@example.com"
    }

    val invalidDataTable = Table(
      ("username","email", "password"),
      ("", "test@example.com", "12345678"),
      ("usernameTest","notAnEmail", "12345678")

    )
    "return bad request for invalid data" in {
      val userDAO = inject[UserDAO]
      val userController = new UserController(stubControllerComponents(), userDAO)(inject[ExecutionContext])

      forAll(invalidDataTable){
        (username, email, password) =>
          val request = FakeRequest(POST, "/signUp")
            .withJsonBody(Json.obj(
              "username" -> username,
              "email" -> email,
              "password" -> password)
            )
            .withCSRFToken

          val result = call(userController.signUp, request)

          status(result) mustBe BAD_REQUEST
      }

    }
  }
}
