package com.beachape.actors

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import com.beachape.helpers.RedisStorageHelper
import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

/**
 * Companion object that houses the factory apply
 * method that returns the Props required to instantiate
 * a [[com.beachape.actors.RedisStringSetToMorphemesOrchestrator]]
 */
object RedisStringSetToMorphemesOrchestrator {

  /**
   * Returns the Props required to spawn an instance of RedisStringSetToMorphemesOrchestrator
   *
   * @param redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new RedisStringSetToMorphemesOrchestrator(redisPool))
}

/**
 * Ochestrator Actor that turns a single request to generate morphemes
 * for a given unix end at time and a given span in seconds (and thus a timespan)
 * and breaks it up in to the smaller individual relevant unix time spans
 * to be processed by RedisStringSetToMorphemesActors in parallel.
 *
 * Receives GenerateMorphemesFor messages and breaks it down into
 * the period requested (termed 'new observed set'), the period immediately before
 * with the same span (termed 'new expected set'), and the corresponding versions
 * of them 24 hours ago ('old observed set' and 'old expected set'). These
 * spans are then sent to a round robin of RedisStringSetToMorphemesActors to be processed
 * so that the morphemes for those respective periods are counted up and cached.
 * The receive method then sends back to the sender the Redis keys at which the
 * morphemes have been tallied up for those respective periods as a RedisSetPair
 * (see [[com.beachape.actors.Messages]])
 *
 * Should be instantiated via the Props returned from companion object's apply method.
 */
class RedisStringSetToMorphemesOrchestrator(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600).seconds)

  val redisStringSetToMorphemesActorsRoundRobin = context.actorOf(
    RedisStringSetToMorphemesActor(redisPool).withRouter(SmallestMailboxRouter(4)),
    "redisStringSetToMorphemesActorsRoundRobin")

  def receive = {

    case message: GenerateMorphemesFor => {

      val zender = sender

      val newObservedSetEndScore = message.unixEndAtTime.toDouble
      val newObservedSetStartScore = newObservedSetEndScore - message.spanInSeconds
      val newExpectedSetEndScore = newObservedSetStartScore
      val newExpectedSetStartScore = newExpectedSetEndScore - message.spanInSeconds

      val oldObservedSetEndScore = newObservedSetEndScore - (RichInt(7 * 24).hours.millis / 1000).toDouble // a week ago
      val oldObservedSetStartScore = oldObservedSetEndScore - message.spanInSeconds
      val oldExpectedSetEndScore = oldObservedSetStartScore
      val oldExpectedSetStartScore = oldObservedSetStartScore - message.spanInSeconds

      val newObservedSetUnixTimeSpan = UnixTimeSpan(UnixTime(newObservedSetStartScore.toInt), UnixTime(newObservedSetEndScore.toInt))
      val newExpectedSetUnixTimeSpan = UnixTimeSpan(UnixTime(newExpectedSetStartScore.toInt), UnixTime(newExpectedSetEndScore.toInt))
      val oldObservedSetUnixTimeSpan = UnixTimeSpan(UnixTime(oldObservedSetStartScore.toInt), UnixTime(oldObservedSetEndScore.toInt))
      val oldExpectedSetUnixTimeSpan = UnixTimeSpan(UnixTime(oldExpectedSetStartScore.toInt), UnixTime(oldExpectedSetEndScore.toInt))

      val listOfRedisKeyFutures = List(
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(
          oldExpectedSetUnixTimeSpan,
          message.dropBlacklisted,
          message.onlyWhitelisted)).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(
          oldObservedSetUnixTimeSpan,
          message.dropBlacklisted,
          message.onlyWhitelisted)).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(
          newExpectedSetUnixTimeSpan,
          message.dropBlacklisted,
          message.onlyWhitelisted)).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(
          newObservedSetUnixTimeSpan,
          message.dropBlacklisted,
          message.onlyWhitelisted)).mapTo[RedisKey])

      val futureListOfRedisKeys = Future.sequence(listOfRedisKeyFutures)
      futureListOfRedisKeys map {
        case List(oldExpectedKey: RedisKey, oldObservedKey: RedisKey, newExpectedKey: RedisKey, newObservedKey: RedisKey) => {
          zender ! RedisSetPair(RedisKeySet(oldExpectedKey, oldObservedKey), RedisKeySet(newExpectedKey, newObservedKey))
        }
        case _ => throw new Exception("RedisStringSetToMorphemesOrchestrator did not receive proper Redis Keys pointing to morphemes")
      }
    }

    case _ => println("RedisStringSetToMorphemesOrchestrator says 'huh?'")
  }

}