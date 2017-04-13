package com.github.calvin

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.calvin.configuration.DynamoDBClientProvider
import com.github.calvin.repositories.interpreters.{DynamoForecastRepository, DynamoMemberRepository, DynamoPasswordResetRepository}
import com.github.calvin.rest.Routes
import com.github.calvin.services.members.MemberManager
import com.github.calvin.services.members.interpreters.SimpleMemberManager
import com.github.calvin.services.weather.WeatherManager
import com.github.calvin.services.weather.interpreters.{OpenWeather, SimpleWeatherManager}
import courier.Mailer

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object Server extends App with Routes {
  override implicit val system: ActorSystem = ActorSystem("test-system")
  override implicit val mat: ActorMaterializer = ActorMaterializer()
  override implicit val ec: ExecutionContext = system.dispatcher

  val config = system.settings.config
  val dynamoConfig = new DynamoDBClientProvider(config)
  val dynamoClient = dynamoConfig.get()
  val weatherApi = OpenWeather(config.getString("openweather.api-key"))
  val mailer: Mailer = Mailer(config.getString("email.smtp-server"), config.getInt("email.smtp-port"))
    .auth(true)
    .as(config.getString("email.sender-email"), config.getString("email.password"))
    .startTtls(true)()

  val forecastRepo = new DynamoForecastRepository(dynamoClient, config)
  val memberRepo = new DynamoMemberRepository(dynamoClient, config)
  val pwResetRepo = new DynamoPasswordResetRepository(dynamoClient, config)

  override val weatherManager: WeatherManager = new SimpleWeatherManager(weatherApi, forecastRepo)
  override val memberManager: MemberManager = new SimpleMemberManager(memberRepo, pwResetRepo, mailer)
  override val secretKey: String = config.getString("secrets.jwt-key")
  override val tokenDurationSeconds: Int = config.getInt("secrets.jwt-token-duration-seconds")

  val result = Http().bindAndHandle(routes, "localhost", 9001)
  result.onComplete {
    case Success(serverBinding) =>
      val information = serverBinding.localAddress
      system.log.info(s"Running service @ ${information.getHostString}:${information.getPort}")

    case Failure(cause) =>
      system.log.error(cause.getMessage)
      system.terminate()
  }
}
