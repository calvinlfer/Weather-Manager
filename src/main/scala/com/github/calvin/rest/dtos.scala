package com.github.calvin.rest

object dtos {

  case class IncomingMember(email: String, password: String)

  case class IncomingEmail(email: String)

  case class IncomingReset(resetCode: String, newPassword: String)

  case class ResetInitiated(email: String, message: String)

  case class SuccessfulReset(message: String = "Password successfully reset")

  case class ErrorMessage(message: String)

  case class JwtUserData(email: String)

  case class AccessTokenWrapper(accessToken: String)

  object Validation {
    private val emailRegex = """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r
    private def checkValidEmail(e: String): Boolean = e match {
      case null                                           => false
      case r if r.trim.isEmpty                            => false
      case r if emailRegex.findFirstMatchIn(r).isDefined  => true
      case _                                              => false
    }

    def validateMember(incomingMember: IncomingMember): Boolean =
      incomingMember.email.nonEmpty && incomingMember.password.nonEmpty && checkValidEmail(incomingMember.email)

    def validateEmail(incomingEmail: IncomingEmail): Boolean =
      incomingEmail.email.nonEmpty && checkValidEmail(incomingEmail.email)

    def validateReset(incomingReset: IncomingReset): Boolean =
      incomingReset.resetCode.nonEmpty && incomingReset.newPassword.nonEmpty

    val memberError = "Email and password cannot be empty and email must be valid e.g username@xyz.com"
    val emailError = "Email cannot be empty and email must be valid e.g username@xyz.com"
    val resetError = "Reset code and new password cannot be empty"
  }
}
