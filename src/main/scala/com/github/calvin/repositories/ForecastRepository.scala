package com.github.calvin.repositories

import scala.concurrent.Future

case class ForecastData(username: String, id: Long)

/**
  * Repository to store OpenWeather ids for users
  */
trait ForecastRepository {
  def createForecast(forecastData: ForecastData): Future[ForecastData]
  def findForecasts(username: String): Future[List[ForecastData]]
  def findForecast(forecastData: ForecastData): Future[Option[ForecastData]]
  def deleteForecast(username: String, id: Long): Future[Long]
}
