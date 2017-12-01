package utils

import akka.actor._
import models.ServerError

import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

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
    f: T => Future[Option[Z]]
) extends Actor with ActorLogging {

  override def receive = {
    case params: T => {
      Try(f(params)) match {
        case Success(s) => sender() ! s
        case Failure(ex) => Status.Failure(new ServerError(ex.getMessage))
      }
    }
    case _ => sender() ! new Exception()
  }
}