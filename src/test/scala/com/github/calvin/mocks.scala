package com.github.calvin

import com.github.calvin.repositories._
import com.github.calvin.services.weather.{CurrentWeatherData, Weather}

import scala.concurrent.Future

object mocks {
  class InMemoryMemberRepository(seed: Seq[Member] = Nil) extends MemberRepository {
    private var map = seed.foldLeft(Map.empty[String, Member])((acc, next) => acc + (next.email -> next))

    override def create(member: Member): Future[Member] = {
      map = map + (member.email -> member)
      Future.successful(member)
    }

    override def find(email: String): Future[Option[Member]] =
      Future.successful(map.get(email))

    override def delete(email: String): Future[Boolean] = {
      map = map - email
      Future.successful(true)
    }
  }

  class InMemoryPasswordRepository(seed: Seq[PasswordResetInformation] = Nil) extends PasswordResetRepository {
    private var map = seed.foldLeft(Map.empty[String, PasswordResetInformation])((acc, next)  => acc + (next.resetCode -> next))

    override def create(pi: PasswordResetInformation): Future[PasswordResetInformation] = {
      map = map + (pi.resetCode -> pi)
      Future.successful(pi)
    }

    override def find(resetCode: String): Future[Option[PasswordResetInformation]] =
      Future.successful(map.get(resetCode))

    override def delete(resetCode: String): Future[Boolean] = {
      map = map - resetCode
      Future.successful(true)
    }
  }

  class InMemoryForecastRepository(seed: Seq[ForecastData] = Nil) extends ForecastRepository {
    private var map: Map[(String, Long), ForecastData] = seed.foldLeft(Map.empty[(String, Long), ForecastData]) {
      (acc, next) => acc + ((next.username, next.id) -> next)
    }

    override def createForecast(forecastData: ForecastData): Future[ForecastData] = {
      map = map + ((forecastData.username, forecastData.id) -> forecastData)
      Future.successful(forecastData)
    }

    override def findForecasts(username: String): Future[List[ForecastData]] =
      Future.successful(map.values.filter(fd => fd.username == username).toList)

    override def findForecast(forecastData: ForecastData): Future[Option[ForecastData]] =
      Future.successful(map.get((forecastData.username, forecastData.id)))

    override def deleteForecast(username: String, id: Long): Future[Long] = {
      map = map - ((username, id))
      Future.successful(id)
    }
  }

  class SampleWeather(seed: List[CurrentWeatherData] = Nil) extends Weather {
    override def getCurrentWeather(lat: Double, lng: Double): Future[CurrentWeatherData] =
      Future.successful(seed.find(cwd => cwd.coord.lat == lat && cwd.coord.lon == lng).get)

    override def getCurrentWeather(cityId: Long): Future[CurrentWeatherData] =
      Future.successful(seed.find(cwd => cwd.id == cityId).get)
  }
}