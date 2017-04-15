package com.github.calvin.services.weather.interpreters

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import com.github.calvin.actors.Member.{AddWeatherId, DeleteWeatherById, GetWeatherIds, WeatherIds}
import com.github.calvin.actors.Member.Sharding.EntityEnvelope
import com.github.calvin.services.weather.{CurrentWeatherData, Weather, WeatherManager}

import scala.concurrent.{ExecutionContext, Future}

class ActorBackedWeatherManager(weather: Weather, userShardRegion: ActorRef)
                               (implicit timeout: Timeout, ec: ExecutionContext) extends WeatherManager {

  override def getForecastsByEmail(username: String): Future[List[CurrentWeatherData]] = {
    val weatherDataIds = (userShardRegion ? EntityEnvelope(username, GetWeatherIds)).mapTo[WeatherIds].map(_.set)
    weatherDataIds.flatMap { setLongs =>
      val result = setLongs.toList.map(id => weather.getCurrentWeather(id))
      Future.sequence(result)
    }
  }

  override def addForecastByLatLong(username: String, lat: Double, lng: Double): Future[CurrentWeatherData] =
    for {
      weatherData <- weather.getCurrentWeather(lat, lng)
      _           <- userShardRegion ? EntityEnvelope(username, AddWeatherId(weatherData.id))
    } yield weatherData

  override def removeForecastById(username: String, id: Long): Future[Long] =
    (userShardRegion ? EntityEnvelope(username, DeleteWeatherById(id))).map(_ => id)
}
