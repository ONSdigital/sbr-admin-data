package controllers.v1

import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory
import model.AdminData
import org.joda.time.format.DateTimeFormat
import play.api.{ Configuration, Environment }
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi }
import repository.AdminDataRepository
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by coolit on 28/11/2017.
 */

case class LookupError(msg: String)
case class LookupResult(adminData: Option[AdminData])

class CircuitBreakerTest extends PlaySpec with MockitoSugar {

  implicit def defaultAwaitTimeout: Timeout = 20.seconds
  val dateFormat = "yyyyMM"
  val dateString = "201706"
  val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(dateFormat))

  lazy val messageException = throw new Exception("Unable to get message")
  lazy val noContentTypeException = throw new Exception("Unable to get content type")

  // Use a fixture as we need the circuit breaker to reset after each test
  // http://www.scalatest.org/user_guide/sharing_fixtures
  def setup =
    new {
      val mockAdminDataRepository = mock[AdminDataRepository]
      val config = Configuration(ConfigFactory.load("test/resources/circuit-breaker.conf")) // Or test.conf, if you have test-specific config files
      val messages = new DefaultMessagesApi(Environment.simple(), config, new DefaultLangs(config))
      val controller = new AdminDataController(mockAdminDataRepository, messages)
      val defaultMessages = messages.messages.get("default").getOrElse(throw new Exception("Unable to get messages"))
    }

  "Circuit breaker" must {
    "be able to handle failures in the wrapped method (getFromDb)" in {
      val s = setup
      val exceptionId = "19283746"
      when(s.mockAdminDataRepository.lookup(date, exceptionId)).thenThrow(new RuntimeException())
      val resp = s.controller.lookup(Some("201706"), exceptionId).apply(FakeRequest())
      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentType(resp).getOrElse(noContentTypeException) mustBe "application/json"
      val errorMessage = s.defaultMessages.get("controller.server.error").getOrElse(messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "must stay closed after successful requests" in {
      val s = setup
      val id = "12345"
      when(s.mockAdminDataRepository.lookup(date, id)) thenReturn Future(Some(AdminData(date, id)))
      (1 to 20).foreach { i =>
        val r = s.controller.lookup(Some(dateString), "12345").apply(FakeRequest())
        Await.result(r, 2 second)
      }
      s.controller.breaker.isClosed mustBe true
    }

    "must be opened after 5 failures" in {
      val s = setup
      val exceptionId = "99664411"
      when(s.mockAdminDataRepository.lookup(date, exceptionId)).thenThrow(new RuntimeException())
      (1 to 5).foreach { i =>
        val resp = s.controller.lookup(Some(dateString), exceptionId).apply(FakeRequest())
        Await.result(resp, 2 second)
      }
      s.controller.breaker.isOpen mustBe true
    }
  }
}
