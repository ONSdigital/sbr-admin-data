package controllers.v1

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import akka.util.Timeout
import play.api.i18n.{DefaultLangs, DefaultMessagesApi}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Configuration, Environment}
import org.joda.time.format.DateTimeFormat
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import com.github.nscala_time.time.Imports.YearMonth
import com.typesafe.config.ConfigFactory

import hbase.model.AdminData
import hbase.model.AdminData.REFERENCE_PERIOD_FORMAT
import hbase.repository.AdminDataRepository

class CircuitBreakerTest extends PlaySpec with MockitoSugar {

  implicit def defaultAwaitTimeout: Timeout = 20.seconds
  private val MAX_RESULT_SIZE = 12
  private val dateString = "201706"
  private val date = YearMonth.parse(dateString, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))

  lazy val messageException = throw new Exception("Unable to get message")

  val configuration = Configuration(ConfigFactory.load("application.test.conf")) // Or test.conf, if you have test-specific config files

  // TODO:
  // - Potentially have more separation between the controller + cb in these tests
  // - Test that state goes back to isHalfOpen after reset timeout
  // - Test that callTimeout works

  // Use a fixture as we need the circuit breaker to reset after each test
  def setup =
    new {
      val cache = new TestCache(true) // Pass in true to turn off caching
      val mockAdminDataRepository: AdminDataRepository = mock[AdminDataRepository]
      //      val config = Configuration(ConfigFactory.load("application.conf")) // Or test.conf, if you have test-specific config files
      val messages = new DefaultMessagesApi(Environment.simple(), configuration, new DefaultLangs(configuration))
      val controller = new AdminDataController(mockAdminDataRepository, messages, cache, configuration)
      //      val defaultMessages = messages.messages.get("default").getOrElse(throw new Exception("Unable to get messages"))
      val defaultMessages: Map[String, String] = messages.messages.getOrElse("default", throw new Exception("Unable to get messages"))
    }

  "Circuit breaker" must {
    "be able to handle failures in the wrapped method (getFromDb)" in {
      val s = setup
      val exceptionId = "19283746"
      when(s.mockAdminDataRepository.lookup(Some(date), exceptionId, Some(MAX_RESULT_SIZE))).thenThrow(new RuntimeException())
      val resp = s.controller.lookup(exceptionId, Some("201706"), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      status(resp) mustBe INTERNAL_SERVER_ERROR
      contentType(resp) mustBe Some("application/json")
      val errorMessage = s.defaultMessages.getOrElse("controller.server.error", messageException)
      (contentAsJson(resp) \ "message_en").as[String] mustBe errorMessage
    }

    "must stay closed after successful requests" in {
      val s = setup
      val id = "12345"
      when(s.mockAdminDataRepository.lookup(Some(date), id, None)) thenReturn Future(Some(Seq(AdminData(date, id))))
      val results = (1 to 20).map { i =>
        s.controller.lookup("12345", Some(dateString), None).apply(FakeRequest())
      }
      val futures = Future.sequence(results)
      Await.result(futures, 2 second)

      s.controller.breaker.isClosed mustBe true
    }

    "must be opened after 5 failures" in {
      val s = setup
      val exceptionId = "99664411"
      when(s.mockAdminDataRepository.lookup(Some(date), exceptionId, Some(MAX_RESULT_SIZE))).thenThrow(new RuntimeException())
      val results = (1 to 5).map { i =>
        s.controller.lookup(exceptionId, Some(dateString), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      }
      val futures = Future.sequence(results)
      Await.result(futures, 3 second)
      s.controller.breaker.isOpen mustBe true
    }

    "must be opened after 5 failures and then fail a valid request" in {
      val s = setup
      val exceptionId = "99664411"
      val validId = "112233"
      when(s.mockAdminDataRepository.lookup(Some(date), exceptionId, Some(MAX_RESULT_SIZE))).thenThrow(new RuntimeException())
      when(s.mockAdminDataRepository.lookup(Some(date), validId, Some(MAX_RESULT_SIZE))) thenReturn Future(Some(Seq(AdminData(date, validId))))
      val results = (1 to 5).map { i =>
        s.controller.lookup(exceptionId, Some(dateString), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      }
      val futures = Future.sequence(results)
      Await.result(futures, 3 second)
      s.controller.breaker.isOpen mustBe true
      val validLookup = s.controller.lookup(validId, Some(dateString), Some(MAX_RESULT_SIZE)).apply(FakeRequest())
      status(validLookup) mustBe INTERNAL_SERVER_ERROR
      // Make sure the circuit breaker caught the last lookup due to it being open
      verify(s.mockAdminDataRepository, times(0)).lookup(Some(date), validId, Some(MAX_RESULT_SIZE))
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
