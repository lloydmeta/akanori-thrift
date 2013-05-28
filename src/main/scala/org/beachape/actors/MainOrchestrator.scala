package org.beachape.actors

import akka.actor.{ Actor, Props }
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import akka.event.Logging
import akka.routing.SmallestMailboxRouter
import com.redis._
import akka.util.Timeout
import scala.concurrent.duration._
import com.redis.RedisClient._

object MainOrchestrator {
  def apply(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean, spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int): Props = Props(new MainOrchestrator(redisPool, dropBlacklisted, onlyWhitelisted, spanInSeconds, minOccurrence, minLength, maxLength, top))
}

class MainOrchestrator(val redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean, spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int) extends Actor with RedisStorageHelper {

  import context.dispatcher

  implicit val timeout = Timeout(600 seconds)

  val stringToRedisRoundRobin = context.actorOf(Props(new StringToRedisActor(redisPool)).withRouter(SmallestMailboxRouter(5)), "mainOrchestartorStringToRedisRoundRobin")
  val redisStringSetToMorphemesOrchestrator = context.actorOf(Props(new RedisStringSetToMorphemesOrchestrator(redisPool)))
  val morphemesTrendDetectRoundRobin = context.actorOf(Props(new MorphemesTrendDetectActor(redisPool)).withRouter(SmallestMailboxRouter(2)), "morphemesTrendDetectRoundRobin")
  val trendGeneratorRoundRobin = context.actorOf(Props(new TrendGeneratorActor(redisPool, dropBlacklisted, onlyWhitelisted)).withRouter(SmallestMailboxRouter(2)), "trendGeneratorRoundRobin")

  def receive = {

    case message @ List('storeString, (stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)) => {
      stringToRedisRoundRobin ! message
    }

    case List('generateDefaultTrends) => {
      val cacheKey = RedisKey(defaultTrendCacheKey)
      deleteKey(cacheKey)
      trendGeneratorRoundRobin ! List('generateTrendsFor, (cacheKey, (System.currentTimeMillis / 1000).toInt, spanInSeconds, minOccurrence, minLength, maxLength, top))
    }

    case 'getTrendsDefault => {
      val cacheKey = RedisKey(defaultTrendCacheKey)
      val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
        redis.zrangebyscoreWithScore(cacheKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC) match {
          case Some(x: List[(String, Double)]) => x
          case _ => Nil
        }
      }
      sender ! listOfReverseSortedTermsAndScores
    }

    case List('getTrends, (spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int)) => {
      // If the cachedKey exists, use the cached results and return it
      // Otherwise, call trendGeneratorRoundRobin and wait
      val zender = sender
      val cachedKey = RedisKey(f"$customTrendCacheKeyEndingNow%s-span$spanInSeconds%s-minOccurence$callMinOccurrence%f-minLength-$callMinLength%d-maxLength$callMaxLength%d-callTop$callTop%d")

      if (cachedKeyExists(cachedKey)) {
        val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
          redis.zrangebyscoreWithScore(cachedKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC) match {
            case Some(x: List[(String, Double)]) => x
            case _ => Nil
          }
        }
        sender ! listOfReverseSortedTermsAndScores
      } else {
        val listOfReverseSortedTermsAndScoresFuture = trendGeneratorRoundRobin ? List('generateTrendsFor, (cachedKey, (System.currentTimeMillis / 1000).toInt, spanInSeconds, callMinOccurrence, callMinLength, callMaxLength, callTop))
        listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
          zender ! listOfReverseSortedTermsAndScores
        }
      }
    }

    // Heavy code duplication with the default case
    case message @ List('getTrendsEndingAt, (unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int)) => {
      val zender = sender
      val cachedKey = RedisKey(f"$customTrendCacheKeyEndingNow%s-unixEndAtTime$unixEndAtTime%d-span$spanInSeconds%s-minOccurence$callMinOccurrence%f-minLength-$callMinLength%d-maxLength$callMaxLength%d-callTop$callTop%d")

      if (cachedKeyExists(cachedKey)) {
        val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
          redis.zrangebyscoreWithScore(cachedKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC) match {
            case Some(x: List[(String, Double)]) => x
            case _ => Nil
          }
        }
        sender ! listOfReverseSortedTermsAndScores
      } else {
        val listOfReverseSortedTermsAndScoresFuture = trendGeneratorRoundRobin ? List('generateTrendsFor, (cachedKey, unixEndAtTime, spanInSeconds, callMinOccurrence, callMinLength, callMaxLength, callTop))
        listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
          zender ! listOfReverseSortedTermsAndScores
        }
      }

    }

    case unneededMessage @ _ => println(unneededMessage)
  }

}