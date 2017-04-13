package com.github.calvin.services.members.interpreters

import com.github.calvin.repositories._
import com.github.t3hnar.bcrypt._
import com.github.calvin.services.members.MemberManager

import scala.concurrent.{ExecutionContext, Future}

class SimpleMemberManager(memberRepo: MemberRepository, pwResetRepo: PasswordResetRepository)
                         (implicit ec: ExecutionContext) extends MemberManager {
  val SaltRounds = 9
  override def createMember(email: String, plaintextPassword: String): Future[Boolean] =
    for {
      result        <- memberRepo.find(email)
      createRes     <- if (result.isDefined) Future.successful(false)
                       else memberRepo.create(Member(email, plaintextPassword.bcrypt(SaltRounds))).map(_ => true)
    } yield createRes

  override def authenticateMember(email: String, plaintextPassword: String): Future[Boolean] =
    memberRepo.find(email).map(optMember => optMember.fold(false)(databaseMember =>
      plaintextPassword.isBcrypted(databaseMember.hashedPw))
    )
}
