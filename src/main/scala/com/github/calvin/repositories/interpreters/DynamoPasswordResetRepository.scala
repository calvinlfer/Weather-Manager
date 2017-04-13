package com.github.calvin.repositories.interpreters

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.github.calvin.repositories.{PasswordResetInformation, PasswordResetRepository}
import com.gu.scanamo._
import com.gu.scanamo.error.DynamoReadError.describe
import com.gu.scanamo.syntax._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class DynamoPasswordResetRepository(client: AmazonDynamoDBAsyncClient, config: Config)(implicit ec: ExecutionContext)
  extends PasswordResetRepository {

  val tableName: String = config.getString("tables.password-reset.name")
  val table: Table[PasswordResetInformation] = Table[PasswordResetInformation](tableName)

  override def create(pi: PasswordResetInformation): Future[PasswordResetInformation] = {
    val createInstruction = table.put(pi)
    ScanamoAsync.exec(client)(createInstruction).map(_ => pi)
  }

  override def find(resetCode: String): Future[Option[PasswordResetInformation]] = {
    val getInstruction = table.get('resetCode -> resetCode)
    ScanamoAsync.exec(client)(getInstruction).map { optEither =>
      optEither.map(either =>
        either.fold(e => throw new Exception(s"Error reading result from Dynamo: ${describe(e)}"), identity)
      )
    }
  }

  override def delete(resetCode: String): Future[Boolean] = {
    val deleteInstruction = table.delete('resetCode -> resetCode)
    ScanamoAsync.exec(client)(deleteInstruction).map(_ => true)
  }
}
