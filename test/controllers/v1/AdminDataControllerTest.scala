package controllers.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.i18n.{DefaultMessagesApi, _}
import play.api.libs.json.JsArray
import play.api.mvc.Results
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
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

  private val MAX_RESULT_SIZE = 12

  // TODO:
  // - test cache duration
  // make all value private and in static style

  val dateString = "201706"
  val date: YearMonth = YearMonth.parse(dateString, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))

  val mockAdminDataRepository: AdminDataRepository = mock[AdminDataRepository]
  val cache = new TestCache(false)

  val configuration = Configuration(ConfigFactory.load("application.test.conf")) // Or test.conf, if you have test-specific config files
  val messages = new DefaultMessagesApi(Environment.simple(), configuration, new DefaultLangs(configuration))
  val controller = new AdminDataController(mockAdminDataRepository, messages, cache, configuration)

  lazy val messageException = throw new Exception("Unable to get message")
  //  lazy val noContentTypeException = throw new Exception("Unable to get content type")

  val defaultMessages: Map[String, String] = messages.messages.getOrElse("default", throw new Exception("Unable to get messages"))

  "AdminDataController" must {
    "return a valid result" in {
      val id = "12345678"
      when(mockAdminDataRepository.lookup(Some(date), id.reverse, None)) thenReturn Future(Some(Seq(AdminData(date, id))))
      val resp = controller.lookup(id, Some(dateString), None).apply(FakeRequest())
      status(resp) mustBe OK
      contentType(resp) mustBe Some("application/json")
      val json = contentAsJson(resp).as[JsArray]
      (json(0) \ "id").as[String] mustBe id
      (json(0) \ "period").as[String] mustBe dateString
    }

    "result was cached" in {
      val id = "55667788"
      val lookup = ValidLookup(id.reverse, Some(date), None)
      when(mockAdminDataRepository.lookup(Some(date), id.reverse, None)) thenReturn Future(Some(Seq(AdminData(date, id))))
      val cacheKey = createCacheKey(lookup)
      cache.get[Future[Result]](cacheKey) mustBe None
      val resp = controller.lookup(id, Some(dateString), None).apply(FakeRequest())
      status(resp) mustBe OK
      cache.get[Future[Result]](cacheKey) must not be None
    }

    "return 400 for an invalid period" in {
      val resp = controller.lookup("12345", Some("201713"), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      contentType(resp) mustBe Some("application/json")
      val errorMessage = defaultMessages.getOrElse("controller.invalid.period", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", REFERENCE_PERIOD_FORMAT)
    }

    "return 400 for an invalid id (not correct length)" in {
      val resp = controller.lookup("0", Some("201706"), None).apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      contentType(resp) mustBe Some("application/json")
      val errorMessage = defaultMessages.getOrElse("controller.invalid.id", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "return 404 for an id that doesn't exist" in {
      val notFoundId = "11223344"
      when(mockAdminDataRepository.lookup(Some(date), notFoundId, Some(MAX_RESULT_SIZE))) thenReturn Future(None)
      val resp = controller.lookup(notFoundId.reverse, Some(dateString), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      status(resp) mustBe NOT_FOUND
      contentType(resp) mustBe Some("application/json")
      val errorMessage = defaultMessages.getOrElse("controller.not.found", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", notFoundId)
    }

    "return 500 when an internal server error occurs" in {
      val exceptionId = "19283746"
      when(mockAdminDataRepository.lookup(Some(date), exceptionId, Some(MAX_RESULT_SIZE))).thenThrow(new RuntimeException())
      val resp = controller.lookup(exceptionId, Some("201706"), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentType(resp) mustBe Some("application/json")
      val errorMessage = defaultMessages.getOrElse("controller.server.error", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }
  }
}