package com.github.calvin.repositories.interpreters

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.github.calvin.repositories.{Member, MemberRepository}
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError.describe
import com.gu.scanamo.syntax._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class DynamoMemberRepository(client: AmazonDynamoDBAsyncClient, config: Config)(implicit ec: ExecutionContext)
  extends MemberRepository {

  val tableName: String = config.getString("tables.members.name")
  val table: Table[Member] = Table[Member](tableName)

  override def create(member: Member): Future[Member] = {
    val createInstruction = table.put(member)
    ScanamoAsync.exec(client)(createInstruction).map(_ => member)
  }

  override def find(email: String): Future[Option[Member]] = {
    val getInstruction = table.get('email -> email)
    ScanamoAsync.exec(client)(getInstruction).map { optEither =>
      optEither.map(either =>
        either.fold(e => throw new Exception(s"Error reading result from Dynamo: ${describe(e)}"), identity)
      )
    }
  }

  override def delete(email: String): Future[Boolean] =  {
    val deleteInstruction = table.delete('email -> email)
    ScanamoAsync.exec(client)(deleteInstruction).map(_ => true)
  }
}
