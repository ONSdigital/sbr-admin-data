package models

/**
 * Created by coolit on 16/11/2017.
 * https://stackoverflow.com/questions/38243530/custom-exception-in-scala
 */
sealed trait MyException {
  self: Throwable =>
  val message: String
}

case class ServerError(message: String) extends Exception(message) with MyException
