package hbase.utils

import org.scalatestplus.play.PlaySpec
import com.github.nscala_time.time.Imports.YearMonth

import model.AdminData
import hbase.util.RowKeyUtils

/**
 * RowKeyUtilsTest
 * ----------------
 * Author: ameen
 * Date: 04 November 2017 - 12:59
 * Copyright (c) 2017  Office for National Statistics
 */
object RowKeyUtilsTest extends PlaySpec {

  private val TEST_REFERENCE_PERIOD: YearMonth = YearMonth.parse("201707")
  private val TEST_KEY: String = "123456789"
  private val TEST_VAT_ROWKEY = String.join(RowKeyUtils.DELIMITER, "201707", TEST_KEY)

  /**
   * @throws(classOf[Exception])
   */
  "A Row Key" must {
    "be created and be valid - generated from period + Strings" in {
      val rowKey: String = RowKeyUtils.createRowKey(TEST_REFERENCE_PERIOD, TEST_KEY)
      TEST_VAT_ROWKEY mustEqual rowKey
    }
  }

  /**
   * @throws(classOf[Exception])
   */
  "Create an AdminData object from a row key" must {
    "match elements of AdminData with expected" in {
      val adminData: AdminData = RowKeyUtils.createAdminDataFromRowKey(TEST_VAT_ROWKEY)
      TEST_KEY mustEqual adminData.id
      TEST_REFERENCE_PERIOD mustEqual adminData.referencePeriod
    }
  }

}