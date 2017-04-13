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

class OpenWeather(httpClient: HttpExt, apiKey: String)(implicit mat: ActorMaterializer, ec: ExecutionContext)
  extends Weather with FailFastCirceSupport {

  override def getCurrentWeather(lat: Double, lng: Double): Future[CurrentWeatherData] =
    httpClient
      .singleRequest(HttpRequest(
        method = GET,
        uri = s"http://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lng&APPID=$apiKey&units=metric"))
      .flatMap(httpResponse => Unmarshal(httpResponse).to[CurrentWeatherData])

  override def getCurrentWeather(id: Long): Future[CurrentWeatherData] =
    httpClient
      .singleRequest(HttpRequest(
        method = GET, uri = s"http://api.openweathermap.org/data/2.5/weather?id=$id&APPID=$apiKey&units=metric"))
      .flatMap(httpResponse => Unmarshal(httpResponse).to[CurrentWeatherData])
}

object OpenWeather {
  def apply(apiKey: String)(implicit mat: ActorMaterializer, ec: ExecutionContext): OpenWeather =
    new OpenWeather(Http(mat.system), apiKey)
}
