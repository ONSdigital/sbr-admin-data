package hbase.util

import javax.inject.Singleton

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
class RowKeyUtils {

  val DELIMITER = "~"

  def reverseOption(id: String, flag: Boolean): String = if (flag) { id.reverse } else id

  def createRowKey(referencePeriod: YearMonth, id: String, flag: Boolean): String =
    String.join(DELIMITER, reverseOption(id, flag), referencePeriod.toString(AdminData.REFERENCE_PERIOD_FORMAT))

  def createAdminDataFromRowKey(rowKey: String, reverse: Boolean): AdminData = {
    val compositeRowKeyParts: Array[String] = rowKey.split(DELIMITER)
    val referencePeriod: YearMonth =
      YearMonth.parse(compositeRowKeyParts.last, DateTimeFormat.forPattern(AdminData.REFERENCE_PERIOD_FORMAT))
    val id = compositeRowKeyParts.head
    AdminData(referencePeriod, reverseOption(id, reverse))
  }

}
