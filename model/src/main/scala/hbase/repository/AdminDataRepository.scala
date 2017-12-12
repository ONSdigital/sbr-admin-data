package hbase.repository

import scala.concurrent.Future

import com.github.nscala_time.time.Imports.YearMonth
import play.api.mvc.Result

import hbase.model.AdminData

/**
 * AdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

trait AdminDataRepository {

  def getCurrentPeriod: Future[YearMonth]

  def lookup(referencePeriod: Option[YearMonth], key: String): Future[Option[AdminData]]

  def lookup(key: String, referencePeriod: Option[YearMonth]): Future[Result]

}
