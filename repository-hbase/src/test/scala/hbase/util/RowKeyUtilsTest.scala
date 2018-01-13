package hbase.util

import com.github.nscala_time.time.Imports.YearMonth
import hbase.model.AdminData
import org.scalatest.{ FlatSpec, Matchers }

/**
 * RowKeyUtilsTest
 * ----------------
 * Author: ameen
 * Date: 04 November 2017 - 12:59
 * Copyright (c) 2017  Office for National Statistics
 */
object RowKeyUtilsTest extends FlatSpec with Matchers {

  private val TEST_REFERENCE_PERIOD: YearMonth = YearMonth.parse("201707")
  private val TEST_KEY: String = "123456789"
  private val TEST_VAT_ROW_KEY = String.join(RowKeyUtils.DELIMITER, "201707", TEST_KEY)

  /**
   * @throws Exception
   */
  it must "create a row key that is valid - generated from period + Strings" in {
    val rowKey: String = RowKeyUtils.createRowKey(TEST_REFERENCE_PERIOD, TEST_KEY)
    TEST_VAT_ROW_KEY should equal(rowKey)
  }

  /**
   * @throws Exception
   */
  it must "create AdminData object from rowkey" in {
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(TEST_VAT_ROW_KEY)
    TEST_KEY should equal(adminData.id)
    TEST_REFERENCE_PERIOD should equal(adminData.referencePeriod)
  }

}