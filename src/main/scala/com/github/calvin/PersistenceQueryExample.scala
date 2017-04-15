package com.github.calvin

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory

object PersistenceQueryExample extends App {
  val config = ConfigFactory.load("query")
  implicit val actorSystem: ActorSystem = ActorSystem("weather-manager", config)
  implicit val materializer = ActorMaterializer()
  // Read journal
  val readJournal = PersistenceQuery(actorSystem).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)
  val log = (x: String) => {
    println(x)
    x
  }

  readJournal.persistenceIds()
    .map(log)
    .flatMapMerge(Int.MaxValue, persistentId => readJournal.eventsByPersistenceId(persistentId, 0, Long.MaxValue))
    .map(eventEnvelope => (eventEnvelope.persistenceId, eventEnvelope.event))
    .runWith(Sink.foreach(println))
}
