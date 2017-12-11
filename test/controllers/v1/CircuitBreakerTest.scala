package controllers.v1

import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory
import model.AdminData
import org.joda.time.format.DateTimeFormat
import play.api.{ Configuration, Environment }
import play.api.i18n.{ DefaultLangs, DefaultMessagesApi }
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.FakeRequest
import play.api.test.Helpers._
import akka.util.Timeout
import hbase.repository.AdminDataRepository

import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created by coolit on 28/11/2017.
 */
class CircuitBreakerTest extends PlaySpec with MockitoSugar {

  implicit def defaultAwaitTimeout: Timeout = 20.seconds
  val dateFormat = AdminData.REFERENCE_PERIOD_FORMAT
  val dateString = "201706"
  val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(dateFormat))

  lazy val messageException = throw new Exception("Unable to get message")
  lazy val noContentTypeException = throw new Exception("Unable to get content type")

  // TODO:
  // - Potentially have more separation between the controller + cb in these tests
  // - Test that state goes back to isHalfOpen after reset timeout
  // - Test that callTimeout works

  // Use a fixture as we need the circuit breaker to reset after each test
  def setup =
    new {
      val cache = new TestCache(true) // Pass in true to turn off caching
      val mockAdminDataRepository = mock[AdminDataRepository]
      val config = Configuration(ConfigFactory.load("application.conf")) // Or test.conf, if you have test-specific config files
      val messages = new DefaultMessagesApi(Environment.simple(), config, new DefaultLangs(config))
      val controller = new AdminDataController(mockAdminDataRepository, messages, cache)
      //      val defaultMessages = messages.messages.get("default").getOrElse(throw new Exception("Unable to get messages"))
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
      val results = (1 to 20).map { i =>
        s.controller.lookup(Some(dateString), "12345").apply(FakeRequest())
      }
      val futures = Future.sequence(results)
      Await.result(futures, 2 second)

      s.controller.breaker.isClosed mustBe true
    }

    "must be opened after 5 failures" in {
      val s = setup
      val exceptionId = "99664411"
      when(s.mockAdminDataRepository.lookup(date, exceptionId)).thenThrow(new RuntimeException())
      val results = (1 to 5).map { i =>
        s.controller.lookup(Some(dateString), exceptionId).apply(FakeRequest())
      }
      val futures = Future.sequence(results)
      Await.result(futures, 3 second)
      s.controller.breaker.isOpen mustBe true
    }

    "must be opened after 5 failures and then fail a valid request" in {
      val s = setup
      val exceptionId = "99664411"
      val validId = "112233"
      when(s.mockAdminDataRepository.lookup(date, exceptionId)).thenThrow(new RuntimeException())
      when(s.mockAdminDataRepository.lookup(date, validId)) thenReturn Future(Some(AdminData(date, validId)))
      val results = (1 to 5).map { i =>
        s.controller.lookup(Some(dateString), exceptionId).apply(FakeRequest())
      }
      val futures = Future.sequence(results)
      Await.result(futures, 3 second)
      s.controller.breaker.isOpen mustBe true
      val validLookup = s.controller.lookup(Some(dateString), validId).apply(FakeRequest())
      status(validLookup) mustBe INTERNAL_SERVER_ERROR
      // Make sure the circuit breaker caught the last lookup due to it being open
      verify(s.mockAdminDataRepository, times(0)).lookup(date, validId)
    }

    //    "must fail a db lookup call that takes too long" in {
    //      val s = setup
    //      val longCallId = "1122334455"
    //      lazy val f = { Thread.sleep(2000); Future(Some(AdminData(date, longCallId))) }
    //      when(s.mockAdminDataRepository.lookup(date, longCallId)) thenReturn f
    //      val resp = s.controller.lookup(Some(dateString), longCallId).apply(FakeRequest())
    //      status(resp) mustBe INTERNAL_SERVER_ERROR
    //    }
  }
}
