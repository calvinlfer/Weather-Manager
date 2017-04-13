package com.github.calvin.services.weather

import scala.concurrent.Future

trait WeatherManager {
  def getForecastsByEmail(username: String): Future[List[CurrentWeatherData]]
  def addForecastByLatLong(username: String, lat: Double, lng: Double): Future[CurrentWeatherData]
  def removeForecastById(username: String, id: Long): Future[Long]
}
