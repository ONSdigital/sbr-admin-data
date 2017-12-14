package models

import com.github.nscala_time.time.Imports.YearMonth

/**
 * Created by coolit on 16/11/2017.
 */
case class ValidLookup(id: String, period: Option[YearMonth])
