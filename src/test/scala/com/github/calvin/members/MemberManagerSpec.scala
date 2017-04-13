package com.github.calvin.members

import com.github.calvin.mocks._
import com.github.calvin.services.members.interpreters.SimpleMemberManager
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FunSpec, MustMatchers}

import scala.concurrent.ExecutionContextExecutor

class MemberManagerSpec extends FunSpec with MustMatchers with ScalaFutures {
  implicit val ec: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global

  describe("Simple Member Manager specification") {
    it("allows you to create a member provided they do not already exist (based on email)") {
      val simpleMemberManager = new SimpleMemberManager(new InMemoryMemberRepository(), new InMemoryPasswordRepository())
      import simpleMemberManager._

      val sampleEmail = "calvin@xyz.com"
      val password = "pass"

      whenReady(createMember(sampleEmail, password)) { result => result mustBe true }
      whenReady(createMember(sampleEmail, password)) { result => result mustBe false }
    }

    it("allows a valid member to authenticate and disallows incorrect credentials") {
      val simpleMemberManager = new SimpleMemberManager(new InMemoryMemberRepository(), new InMemoryPasswordRepository())
      import simpleMemberManager._

      val sampleEmail = "calvin@xyz.com"
      val password = "pass"

      whenReady(createMember(sampleEmail, password)) { result => result mustBe true }
      whenReady(authenticateMember(sampleEmail, password)) { result => result mustBe true}
      whenReady(authenticateMember(sampleEmail, "Bad password")) { result => result mustBe false}
    }
  }
}
