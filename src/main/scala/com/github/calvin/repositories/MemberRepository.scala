package com.github.calvin.repositories

import scala.concurrent.Future

case class Member(email: String, hashedPw: String)

/**
  * Repository to handle member authentication
  */
trait MemberRepository {
  def create(member: Member): Future[Member]
  def find(email: String): Future[Option[Member]]
  def delete(email: String): Future[Boolean]
}
