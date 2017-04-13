package com.github.calvin.services.weather.interpreters

import akka.http.scaladsl.model.HttpMethods._
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.ActorMaterializer
import com.github.calvin.services.weather.{CurrentWeatherData, Weather}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps
import scalacache.caffeine.CaffeineCache
import scalacache._
import memoization._

/**
  * OpenWeather implementation of Weather
  * @param httpClient Akka HTTP client
  * @param apiKey OpenWeather API key
  * @param mat Akka Streams Materializer
  * @param ec Execution Context needed to run Futures
  */
class OpenWeather(httpClient: HttpExt, apiKey: String)(implicit mat: ActorMaterializer, ec: ExecutionContext)
  extends Weather with FailFastCirceSupport {
  implicit val scalaCache = ScalaCache(CaffeineCache())

  override def getCurrentWeather(lat: Double, lng: Double): Future[CurrentWeatherData] = memoize(30 minutes) {
    httpClient
      .singleRequest(HttpRequest(
        method = GET,
        uri = s"http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lng&APPID=$apiKey&units=metric"))
      .flatMap(httpResponse => Unmarshal(httpResponse).to[CurrentWeatherData])
  }

  override def getCurrentWeather(id: Long): Future[CurrentWeatherData] = memoize(30 minutes) {
    httpClient
      .singleRequest(HttpRequest(
        method = GET, uri = s"http://api.openweathermap.org/data/2.5/weather?id=$id&APPID=$apiKey&units=metric"))
      .flatMap(httpResponse => Unmarshal(httpResponse).to[CurrentWeatherData])
  }
}

object OpenWeather {
  def apply(apiKey: String)(implicit mat: ActorMaterializer, ec: ExecutionContext): OpenWeather =
    new OpenWeather(Http(mat.system), apiKey)
}
