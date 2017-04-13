package com.github.calvin.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RejectionHandler, Route}
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import com.github.calvin.rest.dtos.Validation._
import com.github.calvin.rest.dtos._
import com.github.calvin.services.members.MemberManager
import com.github.calvin.services.weather.WeatherManager
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

trait Routes extends JwtSupport with FailFastCirceSupport {
  implicit val system: ActorSystem
  implicit val mat: ActorMaterializer
  implicit val ec: ExecutionContext
  val weatherManager: WeatherManager
  val memberManager: MemberManager

  val authenticateEndpoint: Route = path("authenticate") {
    post {
      entity(as[IncomingMember]) { incomingMember =>
        validate(validateMember(incomingMember), memberError) {
          val result = memberManager.authenticateMember(incomingMember.email, incomingMember.password)
          onComplete(result) {
            case Success(res) =>
              if (res) complete(StatusCodes.OK, AccessTokenWrapper(createBase64EncodedJwtToken(incomingMember.email)))
              else complete(Unauthorized, ErrorMessage("Invalid password"))

            case Failure(cause) =>
              println(cause)
              complete(ServiceUnavailable, ErrorMessage("Failed to authenticate"))
          }
        }
      }
    }
  }

  val resetEndpoint: Route = path("reset") {
    post {
      entity(as[IncomingEmail]) { incomingEmail =>
        validate(validateEmail(incomingEmail), emailError) {
          onComplete(memberManager.sendResetEmail(incomingEmail.email)) {
            case Success(result) =>
              if (result) complete(ResetInitiated(incomingEmail.email, "reset code sent to your email"))
              else complete(Unauthorized, ErrorMessage("User with that email does not exist"))

            case Failure(cause) =>
              println(cause)
              complete(ServiceUnavailable, ErrorMessage("Unable to send reset code"))
          }
        }
      }
    }
  }

  val recoveryEndpoint: Route = path("recover") {
    post {
      entity(as[IncomingReset]) { incomingReset =>
        validate(validateReset(incomingReset), resetError) {
          val resetCode = incomingReset.resetCode
          val plaintextPassword = incomingReset.newPassword
          onComplete(memberManager.resetPassword(resetCode, plaintextPassword)) {
            case Success(result) =>
              if (result) complete(SuccessfulReset())
              else complete(Unauthorized, ErrorMessage("Reset code invalid"))

            case Failure(cause) =>
              println(cause)
              complete(ServiceUnavailable, ErrorMessage("Unable to reset password"))
          }
        }
      }
    }
  }

  val signupMember: Route = path("signup") {
    post {
      entity(as[IncomingMember]) { incomingMember =>
        validate(validateMember(incomingMember), memberError) {
          val result = memberManager.createMember(incomingMember.email, incomingMember.password)
          onComplete(result) {
            case Success(res) =>
              if (res) complete(StatusCodes.Created)
              else complete(StatusCodes.Conflict, ErrorMessage("User already exists"))

            case Failure(cause) =>
              println(cause)
              complete(ServiceUnavailable, ErrorMessage("Failed to signup"))
          }
        }
      }
    }
  }

  import akka.http.scaladsl.model.HttpMethods._

  // Add CORS headers for rejected requests
  // https://github.com/lomigmegard/akka-http-cors/issues/5
  val routes: Route =
  handleRejections(corsRejectionHandler) {
    cors(CorsSettings.defaultSettings.copy(allowedMethods = GET :: POST :: HEAD :: OPTIONS :: PUT :: DELETE :: Nil)) {
      handleRejections(RejectionHandler.default) {
        authenticateEndpoint ~ signupMember ~ resetEndpoint ~ recoveryEndpoint
      }
    }
  }
}
