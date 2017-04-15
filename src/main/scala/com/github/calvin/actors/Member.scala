package com.github.calvin.actors

import java.time.ZonedDateTime

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{ActorLogging, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor

import scala.concurrent.duration._
import scala.language.postfixOps
import Member._

class Member extends PersistentActor with ActorLogging {
  var weatherIds = Set.empty[Long]

  val updateState: Event => Unit = {
    case WeatherIdAdded(id) => weatherIds = weatherIds + id
    case WeatherIdDeleted(id) => weatherIds = weatherIds - id
    case _: WeatherIds => ()
    case _: UserSignedIn => ()
    case _: UserResetPassword => ()
  }

  override def persistenceId: String = s"member-${self.path.name}"


  override def preStart(): Unit = {
    log.info(s"Bringing up $persistenceId")
    context.setReceiveTimeout(120 seconds)
  }

  override def receiveRecover: Receive = {
    case e: Event => updateState(e)
  }

  override def receiveCommand: Receive = {
    case AddWeatherId(id) =>
      log.info(s"Adding a Weather ID {}", id)
      persist(WeatherIdAdded(id)) { event =>
        updateState(event)
        sender() ! event
      }

    case DeleteWeatherById(id) =>
      persist(WeatherIdDeleted(id)) { event =>
        updateState(event)
        sender() ! event
      }

    case GetWeatherIds =>
      sender() ! WeatherIds(weatherIds)

    case RecordUserHasSignedIn(time) =>
      persist(UserSignedIn(time))(updateState)

    case RecordUserHasPasswordReset(time) =>
      persist(UserResetPassword(time))(updateState)

    case ReceiveTimeout =>
      context.parent ! Passivate(stopMessage = Stop)

    case Stop =>
      context stop self
  }
}

object Member {
  sealed trait Command
  case class AddWeatherId(id: Long) extends Command
  case class DeleteWeatherById(id: Long) extends Command
  case object GetWeatherIds extends Command
  case class RecordUserHasSignedIn(time: ZonedDateTime) extends Command
  case class RecordUserHasPasswordReset(time: ZonedDateTime) extends Command

  sealed trait Event
  case class UserSignedIn(time: ZonedDateTime) extends Event
  case class UserResetPassword(time: ZonedDateTime) extends Event
  case class WeatherIdAdded(id: Long) extends Event
  case class WeatherIdDeleted(id: Long) extends Event
  case class WeatherIds(set: Set[Long]) extends Event

  object Sharding {
    case class EntityEnvelope(id: String, command: Command)

    val shardName: String = "MemberShard"

    val extractEntityId: ShardRegion.ExtractEntityId = {
      case EntityEnvelope(id, payload) ⇒ (id.toString, payload)
    }

    def shardIdExtractor(numberOfShards: Int): ShardRegion.ExtractShardId = {
      case env: EntityEnvelope => (env.id.hashCode % numberOfShards).toString
    }
  }
}
