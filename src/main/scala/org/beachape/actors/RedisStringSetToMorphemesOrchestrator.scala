package org.beachape.actors

import akka.actor.Actor
import akka.event.Logging
import com.redis._
import org.beachape.analyze.FileMorphemesToRedis
import akka.actor.Props
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import scala.concurrent.{ Await, Future }
import akka.util.Timeout
import scala.concurrent.duration._
import com.github.nscala_time.time.Imports._

class RedisStringSetToMorphemesOrchestrator(redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600).seconds)

  val redisStringSetToMorphemesActorsRoundRobin = context.actorOf(Props(new RedisStringSetToMorphemesActor(redisPool)).withRouter(RoundRobinRouter(4)), "reidsStringSetToMorphemesActorsRoundRobin")

  def receive = {

    case List('generateTrendsFor, (unixEndAtTime: Int, spanInSeconds: Int, dropBlacklisted: Boolean, onlyWhitelisted: Boolean)) => {

      val zender = sender

      val newObservedSetEndScore = unixEndAtTime.toDouble
      val newObservedSetStartScore = newObservedSetEndScore - spanInSeconds
      val newExpectedSetEndScore = newObservedSetStartScore
      val newExpectedSetStartScore = newExpectedSetEndScore - spanInSeconds

      val oldObservedSetEndScore = newObservedSetEndScore - (1.week.millis / 1000).toDouble
      val oldObservedSetStartScore = oldObservedSetEndScore - spanInSeconds
      val oldExpectedSetEndScore = oldObservedSetStartScore
      val oldExpectedSetStartScore = oldObservedSetStartScore - spanInSeconds

      val newObservedSetUnixTimeSpan = UnixTimeSpan(UnixTime(newObservedSetStartScore.toInt), UnixTime(newObservedSetEndScore.toInt))
      val newExpectedSetUnixTimeSpan = UnixTimeSpan(UnixTime(newExpectedSetStartScore.toInt), UnixTime(newExpectedSetEndScore.toInt))
      val oldObservedSetUnixTimeSpan = UnixTimeSpan(UnixTime(oldObservedSetStartScore.toInt), UnixTime(oldObservedSetEndScore.toInt))
      val oldExpectedSetUnixTimeSpan = UnixTimeSpan(UnixTime(oldExpectedSetStartScore.toInt), UnixTime(oldExpectedSetEndScore.toInt))

      val listOfRedisKeyFutures = List(
        ask(redisStringSetToMorphemesActorsRoundRobin, oldExpectedSetUnixTimeSpan).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, oldObservedSetUnixTimeSpan).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, newExpectedSetUnixTimeSpan).mapTo[RedisKey],
        ask(redisStringSetToMorphemesActorsRoundRobin, newObservedSetUnixTimeSpan).mapTo[RedisKey])

      val futureListOfRedisKeys = Future.sequence(listOfRedisKeyFutures)
      futureListOfRedisKeys map { redisKeysList =>
        redisKeysList match {
          case List(oldExpectedKey: RedisKey, oldObservedKey: RedisKey, newExpectedKey: RedisKey, newObservedKey: RedisKey) => {
            zender ! List(RedisKeySet(oldExpectedKey, oldObservedKey), RedisKeySet(newExpectedKey, newObservedKey))
          }
          case _ => exit(1)
        }
      }
    }

    case _ => println("RedisStringSetToMorphemesOrchestrator says 'huh?'")
  }

}