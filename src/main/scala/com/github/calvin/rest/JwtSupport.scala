package com.github.calvin.rest

import java.time.Instant

import akka.http.scaladsl.model.HttpMessage
import akka.http.scaladsl.model.StatusCodes.Unauthorized
import akka.http.scaladsl.server.Directives.{complete, extractRequest}
import akka.http.scaladsl.server.Route
import com.github.calvin.rest.dtos._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import pdi.jwt.JwtAlgorithm._
import pdi.jwt._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._

import scala.util.{Failure, Try}

/**
  * Mixin trait to facilitate handling of JWT tokens
  */
trait JwtSupport extends FailFastCirceSupport {
  val secretKey: String
  val tokenDurationSeconds: Int

  def createBase64EncodedJwtToken(email: String): String = {
    val claim = JwtClaim(
      issuedAt = Some(Instant.now().getEpochSecond),
      expiration = Some(Instant.now().plusSeconds(tokenDurationSeconds).getEpochSecond),
      issuer = Some("weather-manager"),
      content = JwtUserData(email).asJson.noSpaces
    )
    JwtCirce.encode(claim, secretKey, HS256)
  }


  def verifyToken(userAction: JwtUserData => Route): Route = {
    def obtainToken(request: HttpMessage): Try[JwtClaim] = {
      val authorizationHeader = request.getHeader("Authorization")
      if (authorizationHeader.isPresent) {
        val unparsedEncodedToken = authorizationHeader.get().value().split(" ")
        if (unparsedEncodedToken.length != 2) Failure(new Exception("Invalid token"))
        else {
          val encodedToken = unparsedEncodedToken(1)
          JwtCirce.decode(encodedToken, secretKey, Seq(HS256))

        }
      } else Failure(new Exception("Token is not present in Authorization header"))
    }

    extractRequest { request =>
      val optToken = obtainToken(request)
      optToken.fold(
        error => complete(Unauthorized, ErrorMessage(error.getMessage)),
        jwtClaim =>
          decode[JwtUserData](jwtClaim.content).fold(
            error => complete(Unauthorized, ErrorMessage(s"Malformed user data ${error.getMessage}")),
            userAction)
      )
    }
  }
}
