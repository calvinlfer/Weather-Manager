package com.github.calvin.rest

import akka.actor.{ActorRef, Props}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.ActorMaterializer
import com.github.calvin.actors.Member
import com.github.calvin.mocks.{InMemoryForecastRepository, InMemoryMemberRepository, InMemoryPasswordRepository, SampleWeather}
import com.github.calvin.rest.dtos.OutgoingWeather._
import com.github.calvin.rest.dtos.{AccessTokenWrapper, IncomingMember, OutgoingWeather}
import com.github.calvin.services.members.MemberManager
import com.github.calvin.services.members.interpreters.SimpleMemberManager
import com.github.calvin.services.weather._
import com.github.calvin.services.weather.interpreters.SimpleWeatherManager
import com.typesafe.config.Config
import courier.Mailer
import io.circe.generic.auto._
import org.scalatest.{FunSpec, MustMatchers}

import scala.concurrent.ExecutionContext

class RouteSpec extends FunSpec with MustMatchers with Routes with ScalatestRouteTest {
  val (exampleLat, exampleLong) = (1, 1)
  val exampleId = 1
  val exampleWeatherData: CurrentWeatherData = CurrentWeatherData(coord = Coordinate(exampleLat, exampleLong),
    weather = Nil, base = "base", main = WeatherDetails(21, 100, 100, 18, 22), wind = Wind(20, 10),
    clouds = Clouds(10), dt = 1, name = "example", id = exampleId)
  val email = "calvin@xyz.com"
  val password = "pass"

  override implicit val mat: ActorMaterializer = this.materializer
  override val ec: ExecutionContext = this.executor

  val config: Config = system.settings.config
  val userShardRegion: ActorRef = ClusterSharding(system).start(
    typeName = Member.Sharding.shardName,
    entityProps = Props[Member],
    settings = ClusterShardingSettings(system),
    extractEntityId = Member.Sharding.extractEntityId,
    extractShardId = Member.Sharding.shardIdExtractor(config.getInt("members.sharding.number-of-shards"))
  )

  val weather = new SampleWeather(exampleWeatherData :: Nil)
  val forecastRepo = new InMemoryForecastRepository()
  override val weatherManager: WeatherManager = new SimpleWeatherManager(weather, forecastRepo)(ec)

  val memberRepo = new InMemoryMemberRepository()
  val pwResetRepo = new InMemoryPasswordRepository()
  val testMailer: Mailer = Mailer("localhost", 25)()
  override val memberManager: MemberManager = new SimpleMemberManager(memberRepo, pwResetRepo, userShardRegion, testMailer)(ec)

  override val secretKey: String = "super-secret"
  override val tokenDurationSeconds: Int = 36000

  describe("Routes specification") {
    it("allows you to sign up with a valid email and password") {
      Post("/signup", IncomingMember(email, password)) ~> routes ~> check {
        status mustBe Created
      }
    }

    it("allows you to authenticate once you have signed up") {
      Post("/authenticate", IncomingMember(email, password)) ~> routes ~> check {
        status mustBe OK
        contentType mustBe `application/json`
        val jwtAccessToken = responseAs[AccessTokenWrapper]
        jwtAccessToken.accessToken.nonEmpty mustBe true
      }
    }

    it("allows you to add and obtain weather information for a user") {
      Post("/authenticate", IncomingMember(email, password)) ~> routes ~> check {
        status mustBe OK
        contentType mustBe `application/json`
        val jwtAccessToken = responseAs[AccessTokenWrapper]

        val authorizationHeader = RawHeader("Authorization", s"Bearer ${jwtAccessToken.accessToken}")
        Post("/members/me/forecasts", Coordinate(exampleLong, exampleLat)).addHeader(authorizationHeader) ~> routes ~> check {
          status mustBe Created
          contentType mustBe `application/json`
          responseAs[OutgoingWeather] mustBe exampleWeatherData.toWeatherResponse
        }

        Get("/members/me/forecasts").addHeader(authorizationHeader) ~> routes ~> check {
          status mustBe OK
          contentType mustBe `application/json`
          val weatherResponse = responseAs[List[OutgoingWeather]]
          weatherResponse.length mustBe 1
          weatherResponse.head mustBe exampleWeatherData.toWeatherResponse
        }
      }
    }
  }
}
