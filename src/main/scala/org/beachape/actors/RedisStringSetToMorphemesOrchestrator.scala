package org.beachape.actors

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

class RedisStringSetToMorphemesOrchestrator(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600).seconds)

  val redisStringSetToMorphemesActorsRoundRobin = context.actorOf(Props(new RedisStringSetToMorphemesActor(redisPool)).withRouter(SmallestMailboxRouter(4)), "redisStringSetToMorphemesActorsRoundRobin")

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
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(oldExpectedSetUnixTimeSpan, message.dropBlacklisted, message.onlyWhitelisted)).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(oldObservedSetUnixTimeSpan, message.dropBlacklisted, message.onlyWhitelisted)).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(newExpectedSetUnixTimeSpan, message.dropBlacklisted, message.onlyWhitelisted)).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, GenerateMorphemesForSpan(newObservedSetUnixTimeSpan, message.dropBlacklisted, message.onlyWhitelisted)).mapTo[RedisKey])

      val futureListOfRedisKeys = Future.sequence(listOfRedisKeyFutures)
      futureListOfRedisKeys map { redisKeysList =>
        redisKeysList match {
          case List(oldExpectedKey: RedisKey, oldObservedKey: RedisKey, newExpectedKey: RedisKey, newObservedKey: RedisKey) => {
            zender ! List(RedisKeySet(oldExpectedKey, oldObservedKey), RedisKeySet(newExpectedKey, newObservedKey))
          }
          case _ => throw new Exception("RedisStringSetToMorphemesOrchestrator did not receive proper Redis Keys pointing to morphemes")
        }
      }
    }

    case _ => println("RedisStringSetToMorphemesOrchestrator says 'huh?'")
  }

}