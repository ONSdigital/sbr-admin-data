package hbase.repository

import scala.concurrent.Future

import org.slf4j.{ Logger, LoggerFactory }
import com.github.nscala_time.time.Imports.YearMonth

import hbase.model.AdminData

/**
  * AdminDataRepository
  * ----------------
  * Author: haqa
  * Date: 23 October 2017 - 20:41
  * Copyright (c) 2017  Office for National Statistics
  */

trait AdminDataRepository {

  def lookup(referencePeriod: Option[YearMonth], key: String, max: Option[Long]): Future[Option[Seq[AdminData]]]
}

object AdminDataRepository {
  val LOGGER: Logger = LoggerFactory.getLogger(getClass.getName)
  val OPEN_ALERT = "----- circuit breaker opened! -----"
  val CLOSED_ALERT = "----- circuit breaker closed! -----"
  val HALF_OPEN_ALERT = "----- circuit breaker half-open -----"
}
