package controllers.v1

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.pattern.CircuitBreaker
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{ Controller, Result }
import utils.CircuitBreakerActor

import scala.concurrent.duration._
import scala.concurrent.Future
import config.Properties.{ cbCallTimeout, cbMaxFailures, cbResetTimeout }

/**
 * AdminDataControllerUtils
 * ----------------
 * Author: haqa
 * Date: 22 November 2017 - 14:19
 * Copyright (c) 2017  Office for National Statistics
 */
trait ControllerUtils extends Controller with LazyLogging {

  val system = ActorSystem("sbr-admin-data-circuit-breaker")
  implicit val ec = system.dispatcher

  val breaker =
    new CircuitBreaker(
      system.scheduler,
      maxFailures = cbMaxFailures,
      callTimeout = cbCallTimeout,
      resetTimeout = cbResetTimeout
    ).
      onOpen(logger.warn("----- circuit breaker opened! -----")).
      onClose(logger.warn("----- circuit breaker closed! -----")).
      onHalfOpen(logger.warn("----- circuit breaker half-open -----"))

  implicit val timeout = Timeout(2 seconds)

  def getCircuitBreaker[T, Z](f: T => Future[Option[Z]]): ActorRef = system.actorOf(Props(new CircuitBreakerActor(f)))

  /**
   * On a result, use .future, e.g. Ok().future
   * Method source: https://github.com/outworkers/util/blob/develop/util-play/src/main/scala/com/outworkers/util/play/package.scala#L98
   */
  implicit class ResultAugmenter(val res: Result) {
    def future: Future[Result] = Future.successful(res)
  }
}
