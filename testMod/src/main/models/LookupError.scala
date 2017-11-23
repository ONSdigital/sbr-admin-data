package models

/**
 * Created by coolit on 16/11/2017.
 */
sealed trait LookupError {
  val msg: String
}
case class InvalidSource(msg: String) extends LookupError
case class InvalidPeriod(msg: String) extends LookupError
case class InvalidId(msg: String) extends LookupError
