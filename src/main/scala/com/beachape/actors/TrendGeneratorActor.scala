package com.beachape.actors

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import com.beachape.analyze.MorphemesRedisRetriever
import com.beachape.helpers.RedisStorageHelper
import com.redis.RedisClient.DESC
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

import scala.language.postfixOps
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Companion object that houses the factory apply
 * method that returns the Props required to instantiate
 * a [[com.beachape.actors.TrendGeneratorActor]]
 */
object TrendGeneratorActor {

  /**
   * Returns the Props required to spawn an instance of StringToRedisActor
   *
   * @param redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new TrendGeneratorActor(redisPool))
}

/**
 * Actor that receives GenerateAndCacheTrendsFor messages (see [[com.beachape.actors.Messages]]),
 * which and calls the necessary Actors to generate and cache the trendiness of the morphemes
 * in the given time span. The actor replies with a list of morphemes in reverse trendiness.
 *
 * GenerateAndCacheTrendsFor messages will have a unixEndAtTime, spanInSeconds, dropBlacklisted,
 * onlyWhitelisted, and cacheKey in them. TrendGeneratorActor has it's own round robin
 * MorphemesTrendDetectActor as well as a RedisStringSetToMorphemesOrchestrator.
 *
 * Should be instantiated via props returned from the companion object's apply method.
 */
class TrendGeneratorActor(val redisPool: RedisClientPool)
  extends Actor
  with RedisStorageHelper
  with Logging {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600) seconds)

  val redisStringSetToMorphemesOrchestrator = context.actorOf(RedisStringSetToMorphemesOrchestrator(redisPool))
  val morphemesTrendDetectRoundRobin = context.actorOf(
    MorphemesTrendDetectActor(redisPool).withRouter(SmallestMailboxRouter(20)),
    "trendGeneratorActorMorphemesTrendDetectRoundRobin")

  def receive = {

    // Replies with a List[(String, Double)]
    case message: GenerateAndCacheTrendsFor => {
      val zender = sender

      if (cachedKeyExists(message.redisCacheKey)) {
        zender ! retrieveTrendsFromKey(
          message.redisCacheKey,
          message.minLength,
          message.maxLength,
          message.top)
      } else {
        //Get 2 sets of keys that point to the morphemes
        val redisKeySetPairFuture = ask(redisStringSetToMorphemesOrchestrator, GenerateMorphemesFor(
          message.unixEndAtTime,
          message.spanInSeconds,
          message.dropBlacklisted,
          message.onlyWhitelisted))
        redisKeySetPairFuture map {
          case pair: RedisSetPair => {
            generateTrends(message.redisCacheKey, pair.oldSet, pair.newSet, message.minOccurrence)
            // This .toList.flatten.filter works because the type returned by retrieveTrendsFromKey is Option[List[(String, Double)]]
            // So even if we were to filter on an empty list with None, it doesn't blow up doing ._2
            zender ! retrieveTrendsFromKey(
              message.redisCacheKey,
              message.minLength,
              message.maxLength,
              message.top)
          }
          case _ => throw new Exception("TrendGeneratorActor did not receive proper Redis keys pointing to morphemes")
        }
      }
    }

    case _ => logger.error("TrendGeneratorActor says 'huh?'")

  }

  //Using the morphemes for each for the time periods(old observed vs expected and
  // new observed vs expected), generate the ChiSquared scores for each term and store
  private def generateTrends(
    trendsCacheKey: RedisKey,
    oldSet: RedisKeySet,
    newSet: RedisKeySet,
    minOccurrence: Double): Option[RedisKey] = {
    val oldSetMorphemesRetriever = MorphemesRedisRetriever(
      redisPool,
      oldSet.expectedKey.redisKey,
      oldSet.observedKey.redisKey,
      minOccurrence)
    val newSetMorphemesRetriever = MorphemesRedisRetriever(
      redisPool,
      newSet.expectedKey.redisKey,
      newSet.observedKey.redisKey,
      minOccurrence)

    val oldSetExpectedTotalScore = oldSetMorphemesRetriever.totalExpectedSetMorphemesScore
    val oldSetObservedTotalScore = oldSetMorphemesRetriever.totalObservedSetMorphemesScore

    val newSetExpectedTotalScore = newSetMorphemesRetriever.totalExpectedSetMorphemesScore
    val newSetObservedTotalScore = newSetMorphemesRetriever.totalObservedSetMorphemesScore

    val results = newSetMorphemesRetriever.mapEachPageOfObservedTermsWithScores(1000) { termsWithScoresListMaybe =>
      // pass to roundRobin to calculate ChiChi and store in the cachedkey set.
      val listOfChichiSquareCalculationResultFuturesOption = termsWithScoresListMaybe.map { termsWithScoresList =>
        for ((term, newObservedScore) <- termsWithScoresList)
        yield
          ask(morphemesTrendDetectRoundRobin, CalculateAndStoreTrendiness(
          term,
          newObservedScore,
          trendsCacheKey,
          oldSetMorphemesRetriever,
          newSetMorphemesRetriever,
          oldSetObservedTotalScore,
          oldSetExpectedTotalScore,
          newSetObservedTotalScore,
          newSetExpectedTotalScore)).mapTo[Boolean]
      }

      // Need to block until all roundRobins finish processing otherwise
      // we might hit memory problems -> segmentation fault land
      // Warning, it doesn't seem like you can turn it into a Future[List[Boolean]]
      // Using Future.sequence and just call await on that...
      val resultsInternal = listOfChichiSquareCalculationResultFuturesOption.map{listOfChichiSquareCalculationResultFutures =>
        for (f <- listOfChichiSquareCalculationResultFutures)
          yield Await.result(f, timeout.duration)
      }

      resultsInternal match {
        case Some(x: List[Boolean]) => x.forall(_ == true)
        case _ => false
      }
    }
    results match {
      case Some(x: List[Boolean]) if x.forall(_ == true) => Some(trendsCacheKey)
      case _ => None
    }
  }

  private def retrieveTrendsFromKey(cacheKey: RedisKey, minLength: Int, maxLength: Int, top: Int): List[(String, Double)] = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(cacheKey.redisKey, min = 0, limit = None, sortAs = DESC)
    }.toList.
      flatten.
      filter(x => minLength to maxLength contains x._1.length).
      take(top)
  }

}