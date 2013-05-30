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

    // Given a Unix timespan, get the Redis strings out of the storageKey and tally up
    // the morphemes
    case (unixTimeSpan: UnixTimeSpan, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) => {
      val zender = sender
      val redisKey = RedisKey(redisKeyForUnixTimeSpanWithOptions(unixTimeSpan, dropBlacklisted, onlyWhitelisted))
      if (cachedKeyExists(redisKey)) {
        zender ! redisKey
      } else {
        val count = 300 //amount of strings to retrieve at once from the store
        val analyzeAndDumpResultsList: List[Boolean] = (for (offSet <- (0 to countOfTermsInSpan(unixTimeSpan) by count)) yield {
          val listOfAnalyzeAndDumpFuturesForOffSet = listOfTermsInUnixTimeSpan(unixTimeSpan, Some(offSet, count)) map { phrase =>
            ask(morphemeAnalyzerRoundRobin, List('dumpMorphemesToRedis, redisKey, phrase, dropBlacklisted, onlyWhitelisted)).mapTo[Boolean]
          }
          val futureListOfAnalyzeAndDumpResultsForOffSet = Future.sequence(listOfAnalyzeAndDumpFuturesForOffSet)
          Await.result(futureListOfAnalyzeAndDumpResultsForOffSet, timeout.duration).asInstanceOf[List[Boolean]].forall(_ == true)
        })(collection.breakOut)

        analyzeAndDumpResultsList match {
          case x: List[Boolean] if x.forall(_ == true) => {
            setExpiryOnRedisKey(redisKey, (RichInt(15).minutes.millis / 1000).toInt)
            zender ! redisKey
          }
          case _ => throw new Exception("morphemeAnalyzerRoundRobin failed to generate morphemes for that timespan")
        }

      }
    }

    case _ => println("RedisStringSetToMorphemesActor says 'huh?'")
  }

  def listOfTermsInUnixTimeSpan(timeSpan: UnixTimeSpan, limit: Option[(Int, Int)] = None) = {
    redisPool.withClient { redis =>
      redis.zrangebyscore(storedStringsSetKey, timeSpan.start.time.toDouble, true, timeSpan.end.time.toDouble, true, limit) match {
        case Some(x: List[String]) => {
          x map (storedStringToString(_))
        }
        case _ => throw new Exception("morphemeAnalyzerRoundRobin couldn't retrieve strings for that timespan")
      }
    }
  }

  def listOfUnixTimeSpanInSteps(unixTimeSpan: UnixTimeSpan, stepInSeconds: Int = 3600): List[UnixTimeSpan] = {
    val originalUnixTimeRange = (unixTimeSpan.start.time to unixTimeSpan.end.time)
    originalUnixTimeRange.listOfConsecutivePairsInSteps(stepInSeconds) map { double =>
      UnixTimeSpan(UnixTime(double._1), UnixTime(double._2))
    }
  }

  def countOfTermsInSpan(unixTimeSpan: UnixTimeSpan) = {
    redisPool.withClient { redis =>
      redis.zcount(storedStringsSetKey, unixTimeSpan.start.time.toDouble, unixTimeSpan.end.time.toDouble, true, true) match {
        case Some(x: Long) => x.toInt
        case None => 0
      }
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