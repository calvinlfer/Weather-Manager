package com.github.calvin.members

import com.github.calvin.mocks._
import com.github.calvin.services.members.interpreters.SimpleMemberManager
import courier.Mailer
import org.jvnet.mock_javamail.Mailbox
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, MustMatchers}

import scala.concurrent.ExecutionContextExecutor

class MemberManagerSpec extends FunSpec with MustMatchers with ScalaFutures {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  val testMailer: Mailer = Mailer("localhost", 25)()

  describe("Simple Member Manager specification") {
    it("allows you to create a member provided they do not already exist (based on email)") {
      val simpleMemberManager = new SimpleMemberManager(new InMemoryMemberRepository(), new InMemoryPasswordRepository(), testMailer)
      import simpleMemberManager._

      val sampleEmail = "calvin@xyz.com"
      val password = "pass"

      whenReady(createMember(sampleEmail, password)) { result => result mustBe true }
      whenReady(createMember(sampleEmail, password)) { result => result mustBe false }
    }

    it("allows a valid member to authenticate and disallows incorrect credentials") {
      val simpleMemberManager = new SimpleMemberManager(new InMemoryMemberRepository(), new InMemoryPasswordRepository(), testMailer)
      import simpleMemberManager._

      val sampleEmail = "calvin@xyz.com"
      val password = "pass"

      whenReady(createMember(sampleEmail, password)) { result => result mustBe true }
      whenReady(authenticateMember(sampleEmail, password)) { result => result mustBe true}
      whenReady(authenticateMember(sampleEmail, "Bad password")) { result => result mustBe false}
    }

    it("allows a reset code to be sent for an existing user that has forgotten their password") {
      val simpleMemberManager = new SimpleMemberManager(new InMemoryMemberRepository(), new InMemoryPasswordRepository(), testMailer)
      import simpleMemberManager._

      val sampleEmail = "calvin@xyz.com"
      val password = "pass"

      whenReady(createMember(sampleEmail, password)) { result => result mustBe true }
      whenReady(sendResetEmail(sampleEmail)) { result =>
        result mustBe true
        val testInbox = Mailbox.get(sampleEmail)
        testInbox.size() mustBe 1
        val resetMessage = testInbox.get(0)
        resetMessage.getSubject mustBe "reset code"
        resetMessage.getContent.toString.contains("The reset code is ") mustBe true
        testInbox.clear()
      }
    }

    it("allows recovery via reset code") {
      val simpleMemberManager = new SimpleMemberManager(new InMemoryMemberRepository(), new InMemoryPasswordRepository(), testMailer)
      import simpleMemberManager._

      val sampleEmail = "calvin@xyz.com"
      val password = "pass"
      val newPassword = "newPass"

      whenReady(createMember(sampleEmail, password)) { result => result mustBe true }
      whenReady(sendResetEmail(sampleEmail)) { result => result mustBe true }
      val testInbox = Mailbox.get(sampleEmail)
      testInbox.size() mustBe 1
      val resetMessage = testInbox.get(0)
      val resetCode = resetMessage.getContent.toString.drop("The reset code is ".length)

      whenReady(resetPassword(resetCode, newPassword)) { result => result mustBe true }
      whenReady(authenticateMember(sampleEmail, newPassword)) { result => result mustBe true}

      testInbox.clear()
    }
  }
}
