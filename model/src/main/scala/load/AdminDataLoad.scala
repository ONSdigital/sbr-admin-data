package load

//import com.google.inject.ImplementedBy
//import hbase.load.HBaseAdminDataLoader

/**
 * AdminDataLoad
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

//@ImplementedBy(classOf[HBaseAdminDataLoader])
trait AdminDataLoad {

  def load(tableName: String, referencePeriod: String, inputFile: String): Int

}

