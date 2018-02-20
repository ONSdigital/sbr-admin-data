package utils

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

import akka.actor._

import models.ServerError

/**
 * Created by coolit on 16/11/2017.
 */

/**
 *
 * @param f the db call method
 * @tparam T parameters for the db call method
 * @tparam Z return type of the db call method
 */
class CircuitBreakerActor[T, Z](
  f: T => Future[Option[Seq[Z]]]) extends Actor with ActorLogging {

  override def receive = {
    case params: T => {
      Try(f(params)) match {
        case Success(s) => {
          println(s"MSG: in CB now ${System.currentTimeMillis / 1000}")
          sender() ! s
        }
        case Failure(ex) =>
          println(s"MSG: in CB now ${System.currentTimeMillis / 1000}")
          log.error("Failure result from function execution.", ex)
          sender() ! Status.Failure(new ServerError(ex.getMessage))
      }
    }
    case _ => {
      println(s"MSG: in CB now ${System.currentTimeMillis / 1000}")
      sender() ! new Exception()
    }
  }
}