package utils

import javax.inject.Inject

import com.github.nscala_time.time.Imports.YearMonth
import org.joda.time.format.DateTimeFormat
import models._
import play.api.i18n.{ I18nSupport, Messages, MessagesApi }
import play.api.i18n.Messages.Implicits._

import scala.util.{ Failure, Success, Try }

/**
 * Created by coolit on 16/11/2017.
 */
class LookupValidator @Inject() (val messagesApi: MessagesApi) {

  val REFERENCE_PERIOD_FORMAT: String = "yyyyMM"

  def validateLookupParams(id: String, period: Option[String]): Either[LookupError, ValidLookup] = {
    (period, id) match {
      case (p, _) if !validPeriod(p) => Left(InvalidPeriod(messagesApi("controller.invalid.period", REFERENCE_PERIOD_FORMAT)))
      case (_, i) if !validId(i) => Left(InvalidId(messagesApi("controller.invalid.id")))
      case _ => Right(ValidLookup(id, formPeriod(period)))
    }
  }

  def validPeriod(period: Option[String]): Boolean = period match {
    case Some(p) => Try(YearMonth.parse(p, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))) match {
      case Success(_) => true
      case Failure(_) => false
    }
    case None => true
  }

  def stringToYearMonth(p: String): YearMonth = YearMonth.parse(p, DateTimeFormat.forPattern(REFERENCE_PERIOD_FORMAT))

  def formPeriod(p: Option[String]): Option[YearMonth] = p match {
    case Some(p) => Some(stringToYearMonth(p))
    case None => None
  }

  def validId(id: String): Boolean = id.length >= 5 && id.length <= 13
}