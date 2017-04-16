package com.github.calvin.services.members.interpreters

import java.time.ZonedDateTime
import java.util.UUID

import akka.actor.ActorRef
import com.github.calvin.actors.Member.{RecordUserHasPasswordReset, RecordUserHasSignedIn}
import com.github.calvin.actors.Member.Sharding.EntityEnvelope
import com.github.calvin.repositories._
import com.github.t3hnar.bcrypt._
import com.github.calvin.services.members.MemberManager
import courier._

import scala.concurrent.{ExecutionContext, Future}

class SimpleMemberManager(memberRepo: MemberRepository, pwResetRepo: PasswordResetRepository, userShardRegion: ActorRef,
                          mailer: Mailer)(implicit ec: ExecutionContext) extends MemberManager {
  val SaltRounds = 9

  private def now(): ZonedDateTime = ZonedDateTime.now()

  override def createMember(email: String, plaintextPassword: String): Future[Boolean] =
    for {
      result        <- memberRepo.find(email)
      createRes     <- if (result.isDefined) Future.successful(false)
                       else memberRepo.create(Member(email, plaintextPassword.bcrypt(SaltRounds))).map(_ => true)
    } yield createRes

  override def authenticateMember(email: String, plaintextPassword: String): Future[Boolean] =
    memberRepo.find(email).map(optMember =>
      optMember.fold(false) { databaseMember =>
        val authResult = plaintextPassword.isBcrypted(databaseMember.hashedPw)
        // record the fact that a user has authenticated
        if (authResult) userShardRegion ! EntityEnvelope(email, RecordUserHasSignedIn(now()))
        authResult
      }
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
        // record the fact that the user has reset their password
        userShardRegion ! EntityEnvelope(email, RecordUserHasPasswordReset(now()))
        memberRepo.create(Member(email, newPlaintextPassword.bcrypt(SaltRounds)))
          .flatMap(_ => pwResetRepo.delete(resetCode))

      case None =>
        Future.successful(false)
    }
  }
}
