package com.github.calvin.weather

import com.github.calvin.mocks.{InMemoryForecastRepository, SampleWeather}
import com.github.calvin.repositories.ForecastData
import com.github.calvin.services.weather._
import com.github.calvin.services.weather.interpreters.SimpleWeatherManager
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, MustMatchers}

import scala.concurrent.ExecutionContextExecutor

class WeatherManagerSpec extends FunSpec with MustMatchers with ScalaFutures {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  describe("Weather Manager specification") {
    val (exampleLat, exampleLong) = (1, 1)
    val exampleId = 1
    val exampleWeatherData: CurrentWeatherData = CurrentWeatherData(coord = Coordinate(exampleLat, exampleLong),
      weather = Nil, base = "base", main = WeatherDetails(21, 100, 100, 18, 22), wind = Wind(20, 10),
      clouds = Clouds(10), dt = 1, name = "example", id = exampleId)
    val email = "calvin@xyz.com"

    it("allows you to add and get forecasts for a particular email") {
      val weather = new SampleWeather(List(exampleWeatherData))
      val weatherManager = new SimpleWeatherManager(weather, new InMemoryForecastRepository())

      whenReady(weatherManager.addForecastByLatLong(email, exampleLat, exampleLong)) { result =>
        result.id mustBe exampleId
      }

      whenReady(weatherManager.getForecastsByEmail(email)) { results =>
        results mustBe exampleWeatherData :: Nil
      }
    }

    it("allows you to remove existing weather ids for an email") {
      val weather = new SampleWeather(List(exampleWeatherData))
      val weatherManager = new SimpleWeatherManager(weather, new InMemoryForecastRepository(List(ForecastData(email, exampleId))))
      whenReady(weatherManager.removeForecastById(email, exampleId)) { result => result mustBe exampleId }
      whenReady(weatherManager.getForecastsByEmail(email)) { results => results mustBe Nil }
    }
  }
}
