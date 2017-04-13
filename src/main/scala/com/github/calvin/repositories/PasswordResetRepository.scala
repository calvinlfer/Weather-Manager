package com.github.calvin.repositories

import scala.concurrent.Future

case class PasswordResetInformation(resetCode: String, email: String)

/**
  * Repository to handle password resets
  */
trait PasswordResetRepository {
  def create(pi: PasswordResetInformation): Future[PasswordResetInformation]
  def find(resetCode: String): Future[Option[PasswordResetInformation]]
  def delete(resetCode: String): Future[Boolean]
}
