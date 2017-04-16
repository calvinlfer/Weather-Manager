package com.github.calvin

import akka.actor.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.PersistenceQuery
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import com.github.calvin.actors.Member.{Event => MemberEvent}
import com.typesafe.config.{Config, ConfigFactory}

class PersistenceQueryExample {
  val config: Config = ConfigFactory.load("query")
  implicit val actorSystem: ActorSystem = ActorSystem("weather-manager", config)
  implicit val materializer = ActorMaterializer()
  // Read journal
  val readJournal: CassandraReadJournal = PersistenceQuery(actorSystem).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  readJournal.persistenceIds()
    .flatMapMerge(Int.MaxValue, persistentId => readJournal.eventsByPersistenceId(persistentId, 0, Long.MaxValue))
    .filter(e => e.event.isInstanceOf[MemberEvent])
    .map(eventEnvelope => (eventEnvelope.persistenceId, eventEnvelope.event))
    .runWith(Sink.foreach(println))
}
