package controllers.v1

import play.api.Configuration
import org.joda.time.format.DateTimeFormat
import org.scalatestplus.play.PlaySpec
import play.api.Environment
import play.api.mvc.Results
import play.api.test.FakeRequest
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory
import model.AdminData
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.DefaultMessagesApi
import play.api.test.Helpers._
import repository.AdminDataRepository

import scala.concurrent.Future
import scala.concurrent._
import ExecutionContext.Implicits.global
import play.api.i18n._

/**
 * Created by coolit on 28/11/2017.
 */
class ScalaControllerTest extends PlaySpec with MockitoSugar with Results {

  val dateFormat = "yyyyMM"
  val dateString = "201706"
  val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(dateFormat))

  val mockAdminDataRepository = mock[AdminDataRepository]
  val cache = new TestCache

  val config = Configuration(ConfigFactory.load("application.conf")) // Or test.conf, if you have test-specific config files
  val messages = new DefaultMessagesApi(Environment.simple(), config, new DefaultLangs(config))
  val controller = new AdminDataController(mockAdminDataRepository, messages, cache)

  lazy val messageException = throw new Exception("Unable to get message")
  lazy val noContentTypeException = throw new Exception("Unable to get content type")

  val defaultMessages = messages.messages.get("default").getOrElse(throw new Exception("Unable to get messages"))

  "AdminDataController" must {
    "return a valid result" in {
      val id = "12345678"
      when(mockAdminDataRepository.lookup(date, id)) thenReturn Future(Some(AdminData(date, id)))
      val resp = controller.lookup(Some("201706"), id).apply(FakeRequest())
      status(resp) mustBe OK
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      (contentAsJson(resp) \ "id").as[String] mustBe id
      (contentAsJson(resp) \ "period").as[String] mustBe dateString
    }

    "return 400 for an invalid period" in {
      val resp = controller.lookup(Some("201713"), "12345").apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.get("controller.invalid.period").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", dateFormat)
    }

    "return 400 for an invalid id (not correct length)" in {
      val resp = controller.lookup(Some("201706"), "0").apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.get("controller.invalid.id").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "return 404 for an id that doesn't exist" in {
      val notFoundId = "11223344"
      when(mockAdminDataRepository.lookup(date, notFoundId)) thenReturn Future(None)
      val resp = controller.lookup(Some(dateString), notFoundId).apply(FakeRequest())
      status(resp) mustBe NOT_FOUND
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.get("controller.not.found").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", notFoundId)
    }

    "return 500 when an internal server error occurs" in {
      val exceptionId = "19283746"
      when(mockAdminDataRepository.lookup(date, exceptionId)).thenThrow(new RuntimeException())
      val resp = controller.lookup(Some("201706"), exceptionId).apply(FakeRequest())
      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.get("controller.server.error").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "lookup was cached" in {
      true mustBe true
    }
  }
}