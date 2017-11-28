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
  val id = "12345678"
  val companyName = "Tesco"
  val notFoundId = "11223344"

  //  val mockMessagesApi = mock[MessagesApi]
  //  mockMessagesApi.messages
  //  when(mockMessagesApi.at("",""))
  //  when(mockMessagesApi.at("", "")).thenReturn("Mock error message");

  val mockAdminDataRepository = mock[AdminDataRepository]
  when(mockAdminDataRepository.lookup(date, id)) thenReturn Future(Some(AdminData(date, id, Map("companyName" -> companyName))))
  when(mockAdminDataRepository.lookup(date, notFoundId)) thenReturn Future(None)

  // Misc caching code
  //  val singletonManager = CacheManager.create()
  //  singletonManager.addCache("testCache")
  //  val testCache = singletonManager.getCache("testCache")
  //  val cacheApi: play.api.cache.CacheApi = new DefaultCacheApi(new TestCache(testCache))
  // val config: Configuration = Configuration.from(Map("i" -> "l"))
  //val cache = new TestCache(Ehcache)

  val cacheApi = mock[play.api.cache.CacheApi]

  val config = Configuration(ConfigFactory.load("application.conf")) // Or test.conf, if you have test-specific config files
  val messages = new DefaultMessagesApi(Environment.simple(), config, new DefaultLangs(config))

  val controller = new AdminDataController(mockAdminDataRepository, messages)

  lazy val messageException = throw new Exception("Unable to get message")

  val defaultMessages = messages.messages.get("default").getOrElse(throw new Exception("Unable to get messages"))

  "AdminDataController" must {
    "return a valid result" in {
      val resp = controller.lookup(Some("201706"), id).apply(FakeRequest())
      status(resp) mustBe OK
      (contentAsJson(resp) \ "id").as[String] mustBe id
      (contentAsJson(resp) \ "period").as[String] mustBe dateString
      (contentAsJson(resp) \ "vars" \ "companyName").as[String] mustBe companyName
    }

    "return 400 for an invalid period" in {
      val resp = controller.lookup(Some("201713"), id).apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      val errorMessage = defaultMessages.get("controller.invalid.period").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", dateFormat)
    }

    "return 400 for an invalid id (not correct length)" in {
      val resp = controller.lookup(Some("201706"), "0").apply(FakeRequest())
      status(resp) mustBe BAD_REQUEST
      val errorMessage = defaultMessages.get("controller.invalid.id").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "return 404 for an id that doesn't exist" in {
      val resp = controller.lookup(Some(dateString), notFoundId).apply(FakeRequest())
      status(resp) mustBe NOT_FOUND
      val errorMessage = defaultMessages.get("controller.not.found").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage.replace("{0}", notFoundId)
    }
  }
}