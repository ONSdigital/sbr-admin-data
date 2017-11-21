package repository

//import java.util.concurrent.CompletionStage

import scala.concurrent.Future

import com.github.nscala_time.time.Imports.YearMonth

import model.AdminData

/**
 * AdminDataRepository
 * ----------------
 * Author: haqa
 * Date: 23 October 2017 - 20:41
 * Copyright (c) 2017  Office for National Statistics
 */

trait AdminDataRepository {

  def getCurrentPeriod: Future[YearMonth]
  //  def getCurrentPeriod: CompletionStage[Option[YearMonth]]

  def lookup(referencePeriod: YearMonth, key: String): Future[Option[AdminData]]
  //  def lookup(referencePeriod: YearMonth, key: String): CompletionStage[AdminData]

}
