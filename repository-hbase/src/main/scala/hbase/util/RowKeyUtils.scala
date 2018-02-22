package hbase.util

import com.github.nscala_time.time.Imports.{ DateTimeFormat, YearMonth }

import hbase.model.AdminData

/**
 * RowKeyUtils
 * ----------------
 * Author: haqa
 * Date: 03 November 2017 - 09:10
 * Copyright (c) 2017  Office for National Statistics
 */
object RowKeyUtils {

  val DELIMITER = "~"

  def createRowKey(referencePeriod: YearMonth, id: String): String =
    String.join(DELIMITER, id.reverse, referencePeriod.toString(AdminData.REFERENCE_PERIOD_FORMAT))

  def createAdminDataFromRowKey(rowKey: String): AdminData = {
    val compositeRowKeyParts: Array[String] = rowKey.split(DELIMITER)
    val referencePeriod: YearMonth =
      YearMonth.parse(compositeRowKeyParts.last, DateTimeFormat.forPattern(AdminData.REFERENCE_PERIOD_FORMAT))
    val id = compositeRowKeyParts.head
    AdminData(referencePeriod, id)
  }

}
