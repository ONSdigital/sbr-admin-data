package utils

import scala.concurrent.Future
import scala.util.Try

import play.api.mvc.Result

/**
 * FutureResponse
 * ----------------
 * Author: haqa
 * Date: 07 December 2017 - 11:47
 * Copyright (c) 2017  Office for National Statistics
 */
object FutureResponse {

  implicit class futureSuccess(val res: Result) {
    def future: Future[Result] = Future.successful(res)
  }

  implicit class futureFail(val ex: Exception) {
    protected def futureErr: Future[Exception] = Future.failed(ex)
  }

  implicit class futureFromTry[T](val f: Try[T]) {
    def futureTryRes: Future[T] = Future.fromTry(f)
  }

}