package utils

import java.time.YearMonth
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.LazyLogging
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.i18n.Messages

import scala.util.{ Failure, Success, Try }
import scalaz.\/
import scalaz.syntax.either._

/**
  * Created by coolit on 16/11/2017.
  */
object LookupValidator {

  // https://stackoverflow.com/questions/12307965/method-parameters-validation-in-scala-with-for-comprehension-and-monads
  // https://gist.github.com/noelwelsh/9cacc8683bf3231b9219
  // Below could use the standard lib Either rather than Scalaz

  val REFERENCE_PERIOD_FORMAT: String = "yyyyMM"

  def validateLookupParams(period: Option[String], id: String): \/[LookupError, ValidLookup] = {
    (period, id) match {
      case (p, _) if !validPeriod(p) => InvalidPeriod(Messages("controller.invalid.period", REFERENCE_PERIOD_FORMAT)).left
      case (_, i) if !validId(i) => InvalidId(Messages("controller.invalid.id")).left
      case _ => period match {
        case Some(p) => LookupSpecificPeriod(stringToYearMonth(period.get), id).right
        case None => LookupDefaultPeriod(id).right
      }
    }
  }

  def validPeriod(period: Option[String]): Boolean = period match {
    case Some(p) => Try(YearMonth.parse(p, DateTimeFormatter.ofPattern(REFERENCE_PERIOD_FORMAT))) match {
      case Success(_) => true
      case Failure(_) => false
    }
    case None => true
  }

  def stringToYearMonth(s: String): YearMonth = YearMonth.parse(s, DateTimeFormatter.ofPattern(REFERENCE_PERIOD_FORMAT))

  def validId(id: String): Boolean = id.length >= 5 && id.length <= 13
}