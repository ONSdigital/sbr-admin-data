package controllers.v1

import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{Controller, Result}

import scala.concurrent.Future

/**
 * AdminDataControllerUtils
 * ----------------
 * Author: haqa
 * Date: 22 November 2017 - 14:19
 * Copyright (c) 2017  Office for National Statistics
 */
trait ControllerUtils extends Controller with LazyLogging {

  /**
    * On a result, use .future, e.g. Ok().future
    * Method source: https://github.com/outworkers/util/blob/develop/util-play/src/main/scala/com/outworkers/util/play/package.scala#L98
    */
  implicit class ResultAugmenter(val res: Result) {
    def future: Future[Result] = Future.successful(res)
  }
}
