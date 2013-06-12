package org.beachape.actors

import scala.concurrent.duration.DurationInt

import org.beachape.analyze.MorphemeScoreRedisHelper

import com.redis.RedisClient.DESC
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

object MainOrchestrator {
  def apply(
    redisPool: RedisClientPool,
    dropBlacklisted: Boolean,
    onlyWhitelisted: Boolean,
    spanInSeconds: Int,
    minOccurrence: Double,
    minLength: Int,
    maxLength: Int,
    top: Int): Props =
    Props(new MainOrchestrator(
      redisPool,
      dropBlacklisted,
      onlyWhitelisted,
      spanInSeconds,
      minOccurrence,
      minLength,
      maxLength, top))
}

/*
 * As the name suggests, the main actor that takes care of sending and receiving
 * messages from other actors.
 * Also acts as a container for the arguments for certain default options
 * passed in from the command line
*/

class MainOrchestrator(
  val redisPool: RedisClientPool,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean,
  spanInSeconds: Int,
  minOccurrence: Double,
  minLength: Int,
  maxLength: Int,
  top: Int) extends Actor
  with RedisStorageHelper
  with MorphemeScoreRedisHelper {

  import context.dispatcher

  implicit val timeout = Timeout(600 seconds)

  val stringToRedisRoundRobin = context.actorOf(
    StringToRedisActor(redisPool).withRouter(SmallestMailboxRouter(5)),
    "mainOrchestratorStringToRedisRoundRobin")
  val trendGeneratorRoundRobin = context.actorOf(
    TrendGeneratorActor(redisPool).withRouter(SmallestMailboxRouter(3)),
    "mainOrchestratorTrendGeneratorRoundRobin")

  def receive = {

    case message: StoreString => {
      stringToRedisRoundRobin ! message
    }

    case GenerateDefaultTrends => {
      val cacheKey = RedisKey(defaultTrendCacheKey)
      deleteKey(cacheKey)
      trendGeneratorRoundRobin ! GenerateAndCacheTrendsFor(
        cacheKey,
        (System.currentTimeMillis / 1000).toInt,
        spanInSeconds,
        minOccurrence,
        minLength,
        maxLength,
        top,
        dropBlacklisted,
        onlyWhitelisted)
    }

    // Replies with Some(List[(String, Double)]
    case GetDefaultTrends => {
      val cacheKey = RedisKey(defaultTrendCacheKey)
      val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
        redis.zrangebyscoreWithScore(cacheKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC)
      }.toList.flatten.filter(x => x._1 != zSetTotalScoreKey)
      sender ! Some(listOfReverseSortedTermsAndScores)
    }

    // Replies with Some(List[(String, Double)]
    // Takes care of retrieving a cached set if it exists
    // If not, generates and caches
    case message: FetchTrendsEndingAt => {
      val zender = sender
      val cachedKey = RedisKey(
        customTrendCacheKey(
          message.unixEndAtTime,
          message.spanInSeconds,
          message.minOccurrence,
          message.minLength,
          message.maxLength,
          message.top,
          message.dropBlacklisted,
          message.onlyWhitelisted))

      if (cachedKeyExists(cachedKey)) {
        val listOfReverseSortedTermsAndScores = redisPool.withClient { redis =>
          redis.zrangebyscoreWithScore(cachedKey.redisKey, Double.NegativeInfinity, limit = None, sortAs = DESC)
        }
        sender ! listOfReverseSortedTermsAndScores
      } else {
        val listOfReverseSortedTermsAndScoresFuture = trendGeneratorRoundRobin ? GenerateAndCacheTrendsFor(
          cachedKey,
          message.unixEndAtTime,
          message.spanInSeconds,
          message.minOccurrence,
          message.minLength,
          message.maxLength,
          message.top,
          message.dropBlacklisted,
          message.onlyWhitelisted)
        listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
          zender ! Some(listOfReverseSortedTermsAndScores)
        }
      }

    }

    case unneededMessage @ _ => println(unneededMessage)
  }

  def customTrendCacheKey(
    unixEndAtTime: Int,
    spanInSeconds: Int,
    callMinOccurrence: Double,
    callMinLength: Int,
    callMaxLength: Int,
    callTop: Int,
    callDropBlacklisted: Boolean,
    callOnlyWhitelisted: Boolean): String = {
    f"$customTrendCacheKeyEndingNow%s-unixEndAtTime$unixEndAtTime%d-span$spanInSeconds%s-minOccurence$callMinOccurrence%f-minLength-$callMinLength%d-maxLength$callMaxLength%d-callTop$callTop%d-callDropBlacklisted$callDropBlacklisted%b-callOnlyWhitelisted$callOnlyWhitelisted%b"
  }

}