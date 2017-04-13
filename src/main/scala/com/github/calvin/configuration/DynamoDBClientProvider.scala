package com.github.calvin.configuration

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.typesafe.config.Config

import scala.util.Try

class DynamoDBClientProvider(config: Config) {
  def get(): AmazonDynamoDBAsyncClient = {
    val optAccessKey = Try(config.getString("dynamodb.aws-access-key-id")).toOption
    val optSecretKey = Try(config.getString("dynamodb.aws-secret-access-key")).toOption
    val optEndpoint = Try(config.getString("dynamodb.endpoint")).toOption

    val dynamoClient: AmazonDynamoDBAsyncClient =
      if (optAccessKey.isDefined && optSecretKey.isDefined) {
        new AmazonDynamoDBAsyncClient(new BasicAWSCredentials(optAccessKey.get, optSecretKey.get))
      } else {
        new AmazonDynamoDBAsyncClient()
      }

    optEndpoint.foreach(dynamoClient.withEndpoint)

    dynamoClient
  }
}
