package org.beachape.actors

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.beachape.analyze.MorphemesRedisRetriever

import com.redis.RedisClient.DESC
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

class TrendGeneratorActor(val redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600) seconds)

  val redisStringSetToMorphemesOrchestrator = context.actorOf(Props(new RedisStringSetToMorphemesOrchestrator(redisPool)))
  val morphemesTrendDetectRoundRobin = context.actorOf(Props(new MorphemesTrendDetectActor(redisPool)).withRouter(SmallestMailboxRouter(30)), "morphemesTrendDetectRoundRobin")

  def receive = {

    case List('generateTrendsFor, (redisCacheKey: RedisKey, unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int)) => {
      val zender = sender
      //Get 2 sets of keys that point to the morphemes
      val listOfRedisKeysFuture = ask(redisStringSetToMorphemesOrchestrator, List('generateMorphemesFor, (unixEndAtTime, spanInSeconds, dropBlacklisted: Boolean, onlyWhitelisted: Boolean)))
      listOfRedisKeysFuture map { listOfRedisKeys =>
        listOfRedisKeys match {
          case List(oldSet: RedisKeySet, newSet: RedisKeySet) => {
            generateTrends(redisCacheKey, oldSet, newSet, callMinOccurrence)
            zender ! retrieveTrendsFromKey(redisCacheKey).filter(x => ((callMinLength to callMaxLength) contains x._1.length)).take(callTop)
          }
          case _ => throw new Exception("TrendGeneratorActor did not receive proper Redis keys pointing to morphemes")
        }
      }
    }

    case _ => println("TrendGeneratorActor says 'huh?'")

  }

  //Using the morphemes for each for the time periods(old observed vs expected and
  // new observed vs expected), generate the ChiSquared scores for each term and store
  def generateTrends(trendsCacheKey: RedisKey, oldSet: RedisKeySet, newSet: RedisKeySet, minOccurrence: Double): RedisKey = {
    val oldSetMorphemesRetriever = MorphemesRedisRetriever(redisPool, oldSet.expectedKey.redisKey, oldSet.observedKey.redisKey, minOccurrence)
    val newSetMorphemesRetriever = MorphemesRedisRetriever(redisPool, newSet.expectedKey.redisKey, newSet.observedKey.redisKey, minOccurrence)

    val oldSetExpectedTotalScore = oldSetMorphemesRetriever.totalExpectedSetMorphemesScore
    val oldSetObservedTotalScore = oldSetMorphemesRetriever.totalObservedSetMorphemesScore

    val newSetExpectedTotalScore = newSetMorphemesRetriever.totalExpectedSetMorphemesScore
    val newSetObservedTotalScore = newSetMorphemesRetriever.totalObservedSetMorphemesScore

    val newObservedSetCard = newSetMorphemesRetriever.observedZCard

    val results = newSetMorphemesRetriever.forEachPageOfObservedTermsWithScores(500) { termsWithScoresList =>
      // pass to roundRobin to calculate ChiChi and store in the cachedkey set.
      val listOfChichiSquareCalculationResultFutures = for ((term, newObservedScore) <- termsWithScoresList) yield {
        ask(morphemesTrendDetectRoundRobin, List(
          'calculateAndStoreTrendiness,
          (term,
            newObservedScore,
            trendsCacheKey,
            oldSetMorphemesRetriever,
            newSetMorphemesRetriever,
            oldSetObservedTotalScore,
            oldSetExpectedTotalScore,
            newSetObservedTotalScore,
            newSetExpectedTotalScore))).mapTo[Boolean]
      }

      // Need to block until all roundRobins finish processing otherwise
      // we might hit memory problems -> segmentation fault land
      // Warning, it doesn't seem like you can turn it into a Future[List[Boolean]]
      // Using Future.sequence and just call await on that...
      val results = for (f <- listOfChichiSquareCalculationResultFutures) yield Await.result(f, timeout.duration).asInstanceOf[Boolean]

      results.forall(x => x == true)
    }
    if (results.forall(x => x == true))
      trendsCacheKey
    else
      throw new Exception("TrendGeneratorActor failed to generate trends")
  }

  def retrieveTrendsFromKey(cacheKey: RedisKey, limit: Option[(Int, Int)] = None) = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(cacheKey.redisKey, min = 0, limit = limit, sortAs = DESC) match {
        case Some(x: List[(String, Double)]) => x
        case _ => Nil
      }
    }
  }

}