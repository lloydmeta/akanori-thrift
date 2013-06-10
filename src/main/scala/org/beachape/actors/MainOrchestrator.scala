package org.beachape.actors

import scala.concurrent.duration.DurationInt

import com.redis.RedisClient.DESC
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

object MainOrchestrator {
  def apply(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean, spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int): Props = Props(new MainOrchestrator(redisPool, dropBlacklisted, onlyWhitelisted, spanInSeconds, minOccurrence, minLength, maxLength, top))
}

/*
 * As the name suggests, the main actor that takes care of sending and receiving
 * messages from other actors.
 * Also acts as a container for the arguments for certain default options
 * passed in from the command line
*/

class MainOrchestrator(val redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean, spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int) extends Actor with RedisStorageHelper {

  import context.dispatcher

  implicit val timeout = Timeout(600 seconds)

  val stringToRedisRoundRobin = context.actorOf(Props(new StringToRedisActor(redisPool)).withRouter(SmallestMailboxRouter(5)), "mainOrchestratorStringToRedisRoundRobin")
  val trendGeneratorRoundRobin = context.actorOf(Props(new TrendGeneratorActor(redisPool)).withRouter(SmallestMailboxRouter(3)), "trendGeneratorRoundRobin")

  def receive = {

    case message @ List('storeString, (stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)) => {
      stringToRedisRoundRobin ! message
    }

    case List('generateDefaultTrends) => {
      val cacheKey = RedisKey(defaultTrendCacheKey)
      deleteKey(cacheKey)
      trendGeneratorRoundRobin ! List('generateTrendsFor, (cacheKey, (System.currentTimeMillis / 1000).toInt, spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted))
    }

    // Replies with Some(List[(String, Double)]
    case 'getTrendsDefault => {
      val cacheKey = RedisKey(defaultTrendCacheKey)
      val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
        redis.zrangebyscoreWithScore(cacheKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC)
      }
      sender ! listOfReverseSortedTermsAndScores
    }

    // Replies with Some(List[(String, Double)]
    case message @ List('getTrendsEndingAt, (unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int, callDropBlacklisted: Boolean, callOnlyWhitelisted: Boolean)) => {
      val zender = sender
      val cachedKey = RedisKey(customTrendCacheKey(unixEndAtTime, spanInSeconds, callMinOccurrence, callMinLength, callMaxLength, callTop, callDropBlacklisted, callOnlyWhitelisted))

      if (cachedKeyExists(cachedKey)) {
        val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
          redis.zrangebyscoreWithScore(cachedKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC)
        }
        sender ! listOfReverseSortedTermsAndScores
      } else {
        val listOfReverseSortedTermsAndScoresFuture = trendGeneratorRoundRobin ? List('generateTrendsFor, (cachedKey, unixEndAtTime, spanInSeconds, callMinOccurrence, callMinLength, callMaxLength, callTop, callDropBlacklisted, callOnlyWhitelisted))
        listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
          zender ! Some(listOfReverseSortedTermsAndScores)
        }
      }

    }

    case unneededMessage @ _ => println(unneededMessage)
  }

  def customTrendCacheKey(unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int, callDropBlacklisted: Boolean, callOnlyWhitelisted: Boolean) = {
    f"$customTrendCacheKeyEndingNow%s-unixEndAtTime$unixEndAtTime%d-span$spanInSeconds%s-minOccurence$callMinOccurrence%f-minLength-$callMinLength%d-maxLength$callMaxLength%d-callTop$callTop%d-callDropBlacklisted$callDropBlacklisted%b-callOnlyWhitelisted$callOnlyWhitelisted%b"
  }

}