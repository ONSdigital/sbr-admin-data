package model

import org.scalatestplus.play.PlaySpec

/**
 * AdminDataTypeTest
 * ----------------
 * Author: haqa
 * Date: 24 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */
@deprecated("Not Traced", "15 Nov 2017 - feature/model")
class AdminDataTypeTest extends PlaySpec {

  "AdminDataType tests" must {
    "return the name of each specified enum as defined when invoking toString" in {
      AdminDataType.COMPANY_REGISTRATION.toString mustEqual "CH"
      AdminDataType.PAYE.toString mustEqual "PAYE"
      AdminDataType.VAT.toString mustEqual "VAT"
    }

    "return corresponding enum type value from enum name string input in all capitals" in {
      AdminDataType.fromString("CH") mustEqual AdminDataType.COMPANY_REGISTRATION
      AdminDataType.fromString("PAYE") mustEqual AdminDataType.PAYE
      AdminDataType.fromString("VAT") mustEqual AdminDataType.VAT
    }

    "return corresponding enum type value from enum name string input in all lower case" in {
      AdminDataType.fromString("ch") mustEqual AdminDataType.COMPANY_REGISTRATION
      AdminDataType.fromString("paye") mustEqual AdminDataType.PAYE
      AdminDataType.fromString("vat") mustEqual AdminDataType.VAT
    }

    "return UNDEFINED AdminData type for unknown name" in {
      AdminDataType.fromString("XXX") mustEqual AdminDataType.UNDEFINED
    }

    //@todo replace null
    "return default enum when passing null value" in {
      AdminDataType.fromString(null) mustEqual AdminDataType.UNDEFINED
    }
  }

}
