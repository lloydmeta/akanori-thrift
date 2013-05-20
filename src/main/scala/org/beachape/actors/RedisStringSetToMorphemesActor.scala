package org.beachape.actors

import akka.actor.{Actor, Props}
import akka.event.Logging
import com.redis._
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import scala.concurrent.{ Await, Future }
import akka.util.Timeout
import scala.concurrent.duration._

class RedisStringSetToMorphemesActor(redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(600 seconds)

  val morphemeAnalyzerRoundRobin = context.actorOf(Props(new MorphemesAnalyzerActor(redisPool)).withRouter(RoundRobinRouter(10)), "redisStringSetToMorphemesMorphemesAnalyzerRoundRobin")

  def receive = {

    case (unixTimeSpan: UnixTimeSpan, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) => {
      val zender = sender
      val redisKey = redisKeyForUnixTimeSpan(unixTimeSpan)
      val listOfAnalyzeAndDumpFutures = listOfTermsInRedisStoredSetBetweenUnixTimeSpan(unixTimeSpan) map { phrase =>
        ask(morphemeAnalyzerRoundRobin, List('dumpMorphemesToRedis, RedisKey(redisKey), phrase, dropBlacklisted, onlyWhitelisted)).mapTo[Boolean]
      }

      val futureListOfAnalyzeAndDumpResults = Future.sequence(listOfAnalyzeAndDumpFutures)

      futureListOfAnalyzeAndDumpResults map { analyzeAndDumpResultsList =>
        analyzeAndDumpResultsList match {
          case x: List[Boolean] if x.forall(_ == true) => {
            zender ! RedisKey(redisKey)
          }
          case _ => exit(1)
        }
      }
    }

    case _ => println("RedisStringSetToMorphemesActor says 'huh?'")
  }

  def listOfTermsInRedisStoredSetBetweenUnixTimeSpan(timeSpan: UnixTimeSpan): List[String] = {
    redisPool.withClient { redis =>
      redis.zrangebyscore(storedStringsSetKey, timeSpan.start.time.toDouble, true, timeSpan.end.time.toDouble, true, None) match {
        case Some(x: List[String]) => {
          x map (storedStringToString(_))
        }
        case _ => Nil
      }
    }

  }

  def redisKeyForUnixTimeSpan(timeSpan: UnixTimeSpan) = f"trends:$timeSpan%s"

}