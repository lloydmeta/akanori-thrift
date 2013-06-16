package com.beachape.actors

import scala.concurrent.duration.DurationInt

import com.beachape.helpers.MorphemeScoreRedisHelper
import com.beachape.helpers.RedisStorageHelper

import com.redis.RedisClient.DESC
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

/**
 * Companion object housing the factory for Props used to instantiate
 *  [[com.beachape.actors.MainOrchestrator]]
 */
object MainOrchestrator {

  /**
   * Returns the props necessary to spawn a
   * new orchestrator  with a given RedisClientPool,
   * dropBlacklisted, onlyWhitelisted, spanInSeconds, minOccurrence, minLength
   * maxLength and top as default trend creation options
   *
   * @param redisPool the RedisClientPool to use
   * @param dropBlacklisted whether to drop blacklisted morphemes by default
   * @param onlyWhitelisted whether to only use whitelisted morphemes by default
   * @param spanInSeconds default span in seconds to the past from current time
   * @param minOccurrence default minimum times that a morpheme has to have
   * occurred in a timespan before it gets counted
   * @param minLength default minimum length of morphemes to retrieve
   * @param maxLength default maximum length of morphemes to retrieve
   * @param top default top most trendy morphemes to retrieve
   *
   */
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

/**
 * Actor that acts as the main interface for the Actor
 * system that takes care of storing strings and extracting
 * and calculating trends
 *
 * Should be instantiated via the factory method in
 * the companion object above
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

    case unneededMessage @ _ => println(unneededMessage)
  }

  /**
   * Returns a string that can be used as a cacheKey for calculated
   * trendiness of morphemes
   *
   * @param spanInSeconds default span in seconds to the past from current time
   * @param minOccurrence default minimum times that a morpheme has to have
   * occurred in a timespan before it gets counted
   * @param minLength default minimum length of morphemes to retrieve
   * @param maxLength default maximum length of morphemes to retrieve
   * @param dropBlacklisted whether to drop blacklisted morphemes by default
   * @param onlyWhitelisted whether to only use whitelisted morphemes by default
   */
  def customTrendCacheKey(
    unixEndAtTime: Int,
    spanInSeconds: Int,
    minOccurrence: Double,
    minLength: Int,
    maxLength: Int,
    top: Int,
    dropBlacklisted: Boolean,
    onlyWhitelisted: Boolean): String = {
    f"$customTrendCacheKeyEndingNow%s-unixEndAtTime$unixEndAtTime%d-span$spanInSeconds%s-minOccurence$minOccurrence%f-minLength-$minLength%d-maxLength$maxLength%d-callTop$top%d-callDropBlacklisted$dropBlacklisted%b-callOnlyWhitelisted$onlyWhitelisted%b"
  }

}