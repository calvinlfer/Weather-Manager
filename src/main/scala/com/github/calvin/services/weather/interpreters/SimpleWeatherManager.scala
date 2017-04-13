package com.github.calvin.services.weather.interpreters

import com.github.calvin.repositories.{ForecastData, ForecastRepository}
import com.github.calvin.services.weather.{CurrentWeatherData, Weather, WeatherManager}

import scala.concurrent.{ExecutionContext, Future}

class SimpleWeatherManager(weather: Weather, forecastRepo: ForecastRepository)(implicit ec: ExecutionContext) extends WeatherManager {
  override def getForecastsByEmail(email: String): Future[List[CurrentWeatherData]] =
    forecastRepo.findForecasts(email).flatMap { list =>
      val result = list.map(forecastData => weather.getCurrentWeather(forecastData.id))
      Future.sequence(result)
    }

  override def addForecastByLatLong(email: String, lat: Double, lng: Double): Future[CurrentWeatherData] =
    for {
      weatherData <- weather.getCurrentWeather(lat, lng)
      _           <- forecastRepo.createForecast(ForecastData(email, weatherData.id))
    } yield weatherData

  override def removeForecastById(username: String, id: Long): Future[Long] =
    forecastRepo.deleteForecast(username, id)
}

