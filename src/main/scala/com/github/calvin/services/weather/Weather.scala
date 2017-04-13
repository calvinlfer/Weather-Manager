package com.github.calvin.services.weather

import scala.concurrent.Future

case class Coordinate(lon: Double, lat: Double)
case class WeatherDescription(id: Int, main: String, description: String, icon: String)
case class WeatherDetails(temp: Double, pressure: Double, humidity: Int, temp_min: Double, temp_max: Double)
case class Wind(speed: Double, deg: Double)
case class Clouds(all: Int)
case class CurrentWeatherData(coord: Coordinate, weather: List[WeatherDescription], base: String, main: WeatherDetails,
                              wind: Wind, clouds: Clouds, dt: Long, name: String, id: Long)

trait Weather {
  def getCurrentWeather(lat: Double, lng: Double): Future[CurrentWeatherData]
  def getCurrentWeather(id: Long): Future[CurrentWeatherData]
}
