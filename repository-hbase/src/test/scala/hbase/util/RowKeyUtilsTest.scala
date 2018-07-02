package hbase.util

import org.scalatest.{ FlatSpec, Matchers }
import com.github.nscala_time.time.Imports.YearMonth

import hbase.model.AdminData

/**
 * RowKeyUtilsTest
 * ----------------
 * Author: ameen
 * Date: 04 November 2017 - 12:59
 * Copyright (c) 2017  Office for National Statistics
 */
object RowKeyUtilsTest extends FlatSpec with Matchers {

  private val RAW_YEAR_MONTH = "201707"
  private val TEST_REFERENCE_PERIOD: YearMonth = YearMonth.parse(RAW_YEAR_MONTH)
  private val TEST_KEY: String = "123456789"
  private val TEST_KEY_REVERSED: String = TEST_KEY.reverse
  private val TEST_VAT_ROWKEY = String.join(RowKeyUtils.DELIMITER, TEST_KEY, RAW_YEAR_MONTH)
  private val TEST_VAT_ROWKEY_REVERSED = String.join(RowKeyUtils.DELIMITER, TEST_KEY_REVERSED, RAW_YEAR_MONTH)

  it must "create a row key that is valid - generated from period + Strings" in {
    val reverseFlag = false
    val rowKey: String = RowKeyUtils.createRowKey(TEST_REFERENCE_PERIOD, TEST_KEY, reverseFlag)
    TEST_VAT_ROWKEY should equal(rowKey)
  }

  it must "create a rowkey with id reversed due to set flag" in {
    val reverseFlag = true
    val rowKey: String = RowKeyUtils.createRowKey(TEST_REFERENCE_PERIOD, TEST_KEY, reverseFlag)
    TEST_VAT_ROWKEY_REVERSED should equal(rowKey)
  }

  it must "create AdminData object from rowkey" in {
    val reverseFlag = false
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(TEST_VAT_ROWKEY, reverseFlag)
    TEST_KEY should equal(adminData.id)
    TEST_REFERENCE_PERIOD should equal(adminData.referencePeriod)
  }

  it must "create AdminData object from a reversed id rowKey and un-reverse it" in {
    val reverseFlag = true
    val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(TEST_VAT_ROWKEY_REVERSED, reverseFlag)
    TEST_KEY should equal(adminData.id)
    TEST_KEY_REVERSED should not equal adminData.id
    TEST_REFERENCE_PERIOD should equal(adminData.referencePeriod)
  }

}