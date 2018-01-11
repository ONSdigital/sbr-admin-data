package hbase

import org.scalatest.FlatSpec

import hbase.connector.{ HBaseConnector, HBaseInMemoryConnector }

/**
 * AbstractHBaseIT
 * ----------------
 * Author: haqa
 * Date: 30 November 2017 - 09:12
 * Copyright (c) 2017  Office for National Statistics
 */
trait AbstractHBaseIT extends FlatSpec {

  protected val TABLE_NAME = "test_table"

  @throws(classOf[Exception])
  protected def beforeClass = new {
    val HBASE_CONNECTOR: HBaseConnector = new HBaseInMemoryConnector(TABLE_NAME)
    HBASE_CONNECTOR.getConnection
  }

}
