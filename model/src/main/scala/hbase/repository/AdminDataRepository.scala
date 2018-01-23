package hbase.repository

import scala.concurrent.Future

import org.slf4j.{ Logger, LoggerFactory }
import com.github.nscala_time.time.Imports.YearMonth

import hbase.model.AdminData
import hbase.util.ModelProperties

/**
 * AdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

trait AdminDataRepository extends ModelProperties {

  def lookup(referencePeriod: Option[YearMonth], key: String, max: Long = MAX_RESULT_SIZE): Future[Option[Seq[AdminData]]]
}

object AdminDataRepository {

  val LOGGER: Logger = LoggerFactory.getLogger(getClass.getName)
  val OPEN_ALERT = "----- circuit breaker opened! -----"
  val CLOSED_ALERT = "----- circuit breaker closed! -----"
  val HALF_OPEN_ALERT = "----- circuit breaker half-open -----"

}
