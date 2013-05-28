package org.beachape.actors

import akka.actor.{ Actor, Props }
import akka.event.Logging
import com.redis._
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import scala.concurrent.{ Await, Future }
import akka.util.Timeout
import scala.concurrent.duration._
import com.github.nscala_time.time.Imports._
import org.beachape.support.RichRange._

class RedisStringSetToMorphemesActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600).seconds)

  val morphemeAnalyzerRoundRobin = context.actorOf(Props(new MorphemesAnalyzerActor(redisPool)).withRouter(SmallestMailboxRouter(3)), "redisStringSetToMorphemesMorphemesAnalyzerRoundRobin")

  def receive = {

    case (unixTimeSpan: UnixTimeSpan, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) => {
      val zender = sender
      val redisKey = RedisKey(redisKeyForUnixTimeSpanWithOptions(unixTimeSpan, dropBlacklisted, onlyWhitelisted))
      if (cachedKeyExists(redisKey)) {
        zender ! redisKey
      } else {
        val listOfAnalyzeAndDumpFutures = listOfUnixTimeSpanInSteps(unixTimeSpan) flatMap {steppedUnixTimeSpan =>
          listOfTermsInRedisStoredSetBetweenUnixTimeSpan(steppedUnixTimeSpan) map { phrase =>
            ask(morphemeAnalyzerRoundRobin, List('dumpMorphemesToRedis, redisKey, phrase, dropBlacklisted, onlyWhitelisted)).mapTo[Boolean]
          }
        }
        val futureListOfAnalyzeAndDumpResults = Future.sequence(listOfAnalyzeAndDumpFutures)

        futureListOfAnalyzeAndDumpResults map { analyzeAndDumpResultsList =>
          analyzeAndDumpResultsList match {
            case x: List[Boolean] if x.forall(_ == true) => {
              setExpiryOnRedisKey(redisKey, (RichInt(15).minutes.millis / 1000).toInt)
              zender ! redisKey
            }
            case _ => throw new Exception("morphemeAnalyzerRoundRobin failed to generate morphemes for that timespan")
          }
        }
      }
    }

    case _ => println("RedisStringSetToMorphemesActor says 'huh?'")
  }

  def listOfUnixTimeSpanInSteps(unixTimeSpan: UnixTimeSpan, stepInSeconds: Int = 1800) : List[UnixTimeSpan] = {
    val originalUnixTimeRange = (unixTimeSpan.start.time to unixTimeSpan.end.time)
    originalUnixTimeRange.listOfConsecutivePairsInSteps(stepInSeconds) map {double =>
      UnixTimeSpan(UnixTime(double._1), UnixTime(double._2))
    }
  }

  def listOfTermsInRedisStoredSetBetweenUnixTimeSpan(timeSpan: UnixTimeSpan): List[String] = {
    redisPool.withClient { redis =>
      redis.zrangebyscore(storedStringsSetKey, timeSpan.start.time.toDouble, true, timeSpan.end.time.toDouble, true, None) match {
        case Some(x: List[String]) => {
          x map (storedStringToString(_))
        }
        case _ => throw new Exception("morphemeAnalyzerRoundRobin couldn't retrieve strings for that timespan")
      }
    }
  }

  def redisKeyForUnixTimeSpanWithOptions(timeSpan: UnixTimeSpan, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) =
    f"trends:$timeSpan%s"

}