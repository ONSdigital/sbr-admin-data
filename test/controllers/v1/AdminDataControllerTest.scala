package controllers.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, _ }

import play.api.{ Configuration, Environment }
import play.api.i18n.{ DefaultMessagesApi, _ }
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Result
import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory

import models.ValidLookup
import hbase.model.AdminData
import hbase.model.AdminData.REFERENCE_PERIOD_FORMAT
import hbase.repository.AdminDataRepository
import utils.Utilities

class AdminDataControllerTest extends PlaySpec with MockitoSugar with Results with Utilities {

  // TODO:
  // - test cache duration

  val dateString = "201706"
  val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))

  val mockAdminDataRepository = mock[AdminDataRepository]
  val cache = new TestCache(false)

  val configuration = Configuration(ConfigFactory.load("application.test.conf")) // Or test.conf, if you have test-specific config files
  val messages = new DefaultMessagesApi(Environment.simple(), configuration, new DefaultLangs(configuration))
  val controller = new AdminDataController(mockAdminDataRepository, messages, cache, configuration)

  lazy val messageException = throw new Exception("Unable to get message")
  lazy val noContentTypeException = throw new Exception("Unable to get content type")

  val defaultMessages = messages.messages.getOrElse("default", throw new Exception("Unable to get messages"))

  "AdminDataController" must {
    "return a valid result" in {
      val id = "12345678"
      when(mockAdminDataRepository.lookup(Some(date), id)) thenReturn Future(Some(AdminData(date, id)))
      val resp = controller.lookup(Some(dateString), id).apply(FakeRequest())
      status(resp) mustBe OK
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      (contentAsJson(resp) \ "id").as[String] mustBe id
      (contentAsJson(resp) \ "period").as[String] mustBe dateString
    }

    "result was cached" in {
      val id = "55667788"
      val lookup = ValidLookup(id, Some(date))
      when(mockAdminDataRepository.lookup(Some(date), id)) thenReturn Future(Some(AdminData(date, id)))
      val cacheKey = createCacheKey(lookup)
      cache.get[Future[Result]](cacheKey) mustBe None
      val resp = controller.lookup(Some(dateString), id).apply(FakeRequest())
      status(resp) mustBe OK
      cache.get[Future[Result]](cacheKey) must not be None
    }

    "return 400 for an invalid period" in {
      val resp = controller.lookup(Some("201713"), "12345").apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.getOrElse("controller.invalid.period", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", REFERENCE_PERIOD_FORMAT)
    }

    "return 400 for an invalid id (not correct length)" in {
      val resp = controller.lookup(Some("201706"), "0").apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.getOrElse("controller.invalid.id", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "return 404 for an id that doesn't exist" in {
      val notFoundId = "11223344"
      when(mockAdminDataRepository.lookup(Some(date), notFoundId)) thenReturn Future(None)
      val resp = controller.lookup(Some(dateString), notFoundId).apply(FakeRequest())
      status(resp) mustBe NOT_FOUND
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.getOrElse("controller.not.found", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", notFoundId)
    }

    "return 500 when an internal server error occurs" in {
      val exceptionId = "19283746"
      when(mockAdminDataRepository.lookup(Some(date), exceptionId)).thenThrow(new RuntimeException())
      val resp = controller.lookup(Some("201706"), exceptionId).apply(FakeRequest())
      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = defaultMessages.getOrElse("controller.server.error", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }
  }
}