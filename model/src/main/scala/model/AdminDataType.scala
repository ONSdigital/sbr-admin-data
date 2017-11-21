package model

/**
 * AdminDataType
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

@deprecated("No trace", "15 Nov 2017 - feature/model")
object AdminDataType extends Enumeration {
  type AdminDataType = Value
  val COMPANY_REGISTRATION: AdminDataType.Value = Value("CH")
  val PAYE: AdminDataType.Value = Value("PAYE")
  val UNDEFINED: AdminDataType.Value = Value("UNDEFINED")
  val VAT: AdminDataType.Value = Value("VAT")

  @deprecated("Migrated to the new fromString", "5 Nov 2017 - feature/repository-hbase")
  final def fromStringOld(s: String): AdminDataType.Value =
    values.find(_.toString.equalsIgnoreCase(s)).getOrElse(UNDEFINED)

  final def fromString(s: String): AdminDataType = Option(withName(s.toUpperCase)).getOrElse(UNDEFINED)
}

