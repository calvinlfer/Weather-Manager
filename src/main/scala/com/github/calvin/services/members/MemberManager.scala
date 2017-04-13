package com.github.calvin.services.members

import scala.concurrent.Future

trait MemberManager {
  def createMember(email: String, plaintextPassword: String): Future[Boolean]
  def authenticateMember(email: String, plaintextPassword: String): Future[Boolean]
}
