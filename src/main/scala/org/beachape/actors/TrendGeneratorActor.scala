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

class TrendGeneratorActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600) seconds)

  val redisStringSetToMorphemesOrchestrator = context.actorOf(Props(new RedisStringSetToMorphemesOrchestrator(redisPool)))
  val morphemesTrendDetectRoundRobin = context.actorOf(Props(new MorphemesTrendDetectActor(redisPool)).withRouter(SmallestMailboxRouter(30)), "morphemesTrendDetectRoundRobin")

  def receive = {

    // Replies with a List[(String, Double)]
    case List('generateTrendsFor, (redisCacheKey: RedisKey, unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int, callDropBlacklisted: Boolean, callOnlyWhitelisted: Boolean)) => {
      val zender = sender
      //Get 2 sets of keys that point to the morphemes
      val listOfRedisKeysFuture = ask(redisStringSetToMorphemesOrchestrator, List('generateMorphemesFor, (unixEndAtTime, spanInSeconds, callDropBlacklisted, callOnlyWhitelisted)))
      listOfRedisKeysFuture map { listOfRedisKeys =>
        listOfRedisKeys match {
          case List(oldSet: RedisKeySet, newSet: RedisKeySet) => {
            generateTrends(redisCacheKey, oldSet, newSet, callMinOccurrence)
            // This .toList.flatten.filter works because the type returned by retrieveTrendsFromKey is Option[List[(String, Double)]]
            // So even if we were to filter on an empty list with None, it doesn't blow up doing ._2
            zender ! retrieveTrendsFromKey(redisCacheKey).toList.flatten.filter(x => ((callMinLength to callMaxLength) contains x._1.length)).take(callTop)
          }
          case _ => throw new Exception("TrendGeneratorActor did not receive proper Redis keys pointing to morphemes")
        }
      }
    }

    case _ => println("TrendGeneratorActor says 'huh?'")

  }

  //Using the morphemes for each for the time periods(old observed vs expected and
  // new observed vs expected), generate the ChiSquared scores for each term and store
  def generateTrends(trendsCacheKey: RedisKey, oldSet: RedisKeySet, newSet: RedisKeySet, minOccurrence: Double): Option[RedisKey] = {
    val oldSetMorphemesRetriever = MorphemesRedisRetriever(redisPool, oldSet.expectedKey.redisKey, oldSet.observedKey.redisKey, minOccurrence)
    val newSetMorphemesRetriever = MorphemesRedisRetriever(redisPool, newSet.expectedKey.redisKey, newSet.observedKey.redisKey, minOccurrence)

    val oldSetExpectedTotalScore = oldSetMorphemesRetriever.totalExpectedSetMorphemesScore
    val oldSetObservedTotalScore = oldSetMorphemesRetriever.totalObservedSetMorphemesScore

    val newSetExpectedTotalScore = newSetMorphemesRetriever.totalExpectedSetMorphemesScore
    val newSetObservedTotalScore = newSetMorphemesRetriever.totalObservedSetMorphemesScore

    val results = newSetMorphemesRetriever.forEachPageOfObservedTermsWithScores(500) { termsWithScoresList =>
      // pass to roundRobin to calculate ChiChi and store in the cachedkey set.
      val listOfChichiSquareCalculationResultFutures = for ((term, newObservedScore) <- termsWithScoresList.getOrElse(Nil)) yield {
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
      val resultsInternal = for (f <- listOfChichiSquareCalculationResultFutures) yield Await.result(f, timeout.duration).asInstanceOf[Boolean]
      resultsInternal.forall(x => x == true)
    }
    if (results.getOrElse(Nil).forall(x => x == true))
      Some(trendsCacheKey)
    else
      None
  }

  def retrieveTrendsFromKey(cacheKey: RedisKey, limit: Option[(Int, Int)] = None) = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(cacheKey.redisKey, min = 0, limit = limit, sortAs = DESC)
    }
  }

}