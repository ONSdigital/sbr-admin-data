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

  @deprecated("Migrated to getAdminData with three param", "13 Dec 2017 - feature/HBase-Rest")
  def lookup(referencePeriod: Option[YearMonth], key: String): Future[Option[AdminData]]

  def lookup(referencePeriod: Option[YearMonth], key: String, max: Long): Future[Option[Seq[AdminData]]]

  @deprecated("Migrated to getAdminData with three param", "13 Dec 2017 - feature/HBase-Rest")
  def lookup(key: String, referencePeriod: Option[YearMonth]): Future[Result]

}
