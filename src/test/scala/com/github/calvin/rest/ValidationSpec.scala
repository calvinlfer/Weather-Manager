package com.github.calvin.rest

import org.scalatest.{FunSpec, MustMatchers}

class ValidationSpec extends FunSpec with MustMatchers {
  import dtos._
  import Validation._

  describe("Validation tests") {
    it("allows members with valid non-empty emails and passwords") {
      val member = IncomingMember(email = "calvin@xyz.com", password = "pass")
      validateMember(member) mustBe true
    }

    it("allows members with valid non-empty emails (for the reset case)") {
      val email = IncomingEmail(email = "calvin@xyz.com")
      validateEmail(email) mustBe true
    }

    it("allows member with non-empty reset codes and passwords (for the recovery case)") {
      val reset = IncomingReset(resetCode = "abc123", newPassword = "lol")
      validateReset(reset) mustBe true
    }

    it("prevents members with empty emails/passwords") {
      val member = IncomingMember(email = "", password = "")
      validateMember(member) mustBe false
    }

    it("prevents members with empty emails (for the reset case)") {
      val email = IncomingEmail(email = "")
      validateEmail(email) mustBe false
    }

    it("prevents members with invalid emails and valid passwords") {
      val member = IncomingMember(email = "ejfeojfeof", password = "pass")
      validateMember(member) mustBe false
    }

    it("prevents members with invalid emails") {
      val email = IncomingEmail(email = "calvin@")
      validateEmail(email) mustBe false
    }

    it("prevents reset codes with empty reset codes or passwords") {
      val reset = IncomingReset(resetCode = "", newPassword = "")
      validateReset(reset) mustBe false
    }
  }
}
