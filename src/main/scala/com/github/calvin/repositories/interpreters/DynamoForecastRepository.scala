package com.github.calvin.repositories.interpreters
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.github.calvin.repositories.{ForecastData, ForecastRepository}
import com.gu.scanamo._
import com.gu.scanamo.syntax._
import com.gu.scanamo.error.DynamoReadError._
import com.typesafe.config.Config

import scala.concurrent.{ExecutionContext, Future}

class DynamoForecastRepository(client: AmazonDynamoDBAsyncClient, config: Config)(implicit ec: ExecutionContext)
  extends ForecastRepository {

  val tableName: String = config.getString("tables.forecast.name")
  val table: Table[ForecastData] = Table[ForecastData](tableName)

  override def findForecasts(username: String): Future[List[ForecastData]] = {
    val getInstruction = table.query('username -> username)
    ScanamoAsync.exec(client)(getInstruction).map { listEither =>
      listEither.map(either =>
        either.fold(e => throw new Exception(s"Error reading result from Dynamo: ${describe(e)}"), identity)
      )
    }
  }

  override def findForecast(forecastData: ForecastData): Future[Option[ForecastData]] = {
    val getInstruction = table.get('username -> forecastData.username and 'id -> forecastData.id)
    ScanamoAsync.exec(client)(getInstruction).map { optEither =>
      optEither.map(either =>
        either.fold(e => throw new Exception(s"Error reading result from Dynamo: ${describe(e)}"), identity)
      )
    }
  }

  override def deleteForecast(username: String, id: Long): Future[Long] = {
    val deleteInstruction = table.delete('username -> username and 'id -> id)
    ScanamoAsync.exec(client)(deleteInstruction).map(_ => id)
  }

  override def createForecast(forecastData: ForecastData): Future[ForecastData] = {
    val createInstruction = table.put(forecastData)
    ScanamoAsync.exec(client)(createInstruction).map(_ => forecastData)
  }
}
