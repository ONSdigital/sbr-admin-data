package hbase.util

import javax.inject.{ Inject, Singleton }

import play.api.Configuration
import com.github.nscala_time.time.Imports.{ DateTimeFormat, YearMonth }

import hbase.model.AdminData

/**
 * RowKeyUtils
 * ----------------
 * Author: haqa
 * Date: 03 November 2017 - 09:10
 * Copyright (c) 2017  Office for National Statistics
 */

@Singleton
class RowKeyUtils @Inject() (val configuration: Configuration) extends HBaseConfigProperties {

  val DELIMITER = "~"

  def reverseOption(id: String, flag: Boolean = reverseFlag): String = if (flag) { id.reverse } else id

  def createRowKey(referencePeriod: YearMonth, id: String, flag: Boolean = reverseFlag): String =
    String.join(DELIMITER, reverseOption(id, flag), referencePeriod.toString(AdminData.REFERENCE_PERIOD_FORMAT))

  def createAdminDataFromRowKey(rowKey: String): AdminData = {
    val compositeRowKeyParts: Array[String] = rowKey.split(DELIMITER)
    val referencePeriod: YearMonth =
      YearMonth.parse(compositeRowKeyParts.last, DateTimeFormat.forPattern(AdminData.REFERENCE_PERIOD_FORMAT))
    val id = compositeRowKeyParts.head
    AdminData(referencePeriod, reverseOption(id))
  }

}
