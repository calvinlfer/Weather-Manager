package com.github.calvin.services.members.interpreters

import java.util.UUID

import com.github.calvin.repositories._
import com.github.t3hnar.bcrypt._
import com.github.calvin.services.members.MemberManager
import courier._

import scala.concurrent.{ExecutionContext, Future}

class SimpleMemberManager(memberRepo: MemberRepository, pwResetRepo: PasswordResetRepository, mailer: Mailer)
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

  override def sendResetEmail(email: String): Future[Boolean] = {
    memberRepo.find(email).flatMap {
      case Some(member) =>
        val name :: domain :: Nil = email.split("@").toList
        val resetCode = UUID.randomUUID().toString
        val envelope = Envelope.from("admin" `@` "weathermanager.com")
          .to(name `@` domain)
          .subject("reset code")
          .content(Text(s"The reset code is $resetCode"))
        pwResetRepo.create(PasswordResetInformation(resetCode, email))
          .flatMap(_ => mailer(envelope).map(_ => true))

      case None =>
        Future.successful(false)
    }
  }

  override def resetPassword(resetCode: String, newPlaintextPassword: String): Future[Boolean] = {
    pwResetRepo.find(resetCode).flatMap {
      case Some(PasswordResetInformation(_, email)) =>
        memberRepo.create(Member(email, newPlaintextPassword.bcrypt(SaltRounds)))
          .flatMap(_ => pwResetRepo.delete(resetCode))

      case None =>
        Future.successful(false)
    }
  }
}
