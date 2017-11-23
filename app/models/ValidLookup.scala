package models

import com.github.nscala_time.time.Imports.YearMonth
import org.joda.time.format.DateTimeFormat

/**
 * Created by coolit on 16/11/2017.
 */
//sealed trait ValidLookup {
//  val id: String
//}
//case class LookupDefaultPeriod(id: String) extends ValidLookup
//case class LookupSpecificPeriod(period: YearMonth, id: String) extends ValidLookup

case class ValidLookup(id: String, period: Option[YearMonth])
//
//object ValidLookup {
//  def apply(): Unit ={
//
//  }
//}
