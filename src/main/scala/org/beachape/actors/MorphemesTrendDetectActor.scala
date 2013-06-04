package org.beachape.actors

import akka.actor.{ Actor, Props }
import akka.event.Logging
import com.redis._
import org.beachape.analyze.MorphemesRedisRetriever
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout
import scala.concurrent.duration._
import com.github.nscala_time.time.Imports._
import scala.language.postfixOps

class MorphemesTrendDetectActor(redisPool: RedisClientPool) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(DurationInt(600).seconds)

  val morphemeRetrieveRoundRobin = context.actorOf(Props(new MorphemeRedisRetrieverActor(redisPool)).withRouter(SmallestMailboxRouter(3)), "morphemeRetrievalRouter")

  def receive = {

    // Given 2 sets of Redis Keys representing morpheme counts [(T1)(T2)] and [(T3, T4)]
    // where Ts are timespans, goes and builds 2 sorted sets of morphemes by chi-squared scores
    // returns the keys
    case List('detectTrends, (oldSet: RedisKeySet, newSet: RedisKeySet, minOccurrence: Double)) => {
      val zender = sender
      val listOfStoredRankedTrendsKeysFutures = List(
        ask(morphemeRetrieveRoundRobin, (oldSet, minOccurrence)).mapTo[RedisKey],
        ask(morphemeRetrieveRoundRobin, (newSet, minOccurrence)).mapTo[RedisKey])

      val futureListOfStoredRankedTrendsKeys = Future.sequence(listOfStoredRankedTrendsKeysFutures)

      futureListOfStoredRankedTrendsKeys map { storedRankKeyList =>
        storedRankKeyList match {
          case List(olderTrendsKey: RedisKey, newerTrendsKey: RedisKey) => {
            zender ! RedisKeySet(olderTrendsKey, newerTrendsKey)
          }
          case _ => throw new Exception("MorphemesTrendDetectActor did not receive proper RedisKeys pointing to ranked morphemes")
        }
      }
    }

    case List('calculateAndStoreChiChi, (
      term: String,
      newObservedScore: Double,
      trendsCacheKey: RedisKey,
      oldSetMorphemesRetriever: MorphemesRedisRetriever,
      newSetMorphemesRetriever: MorphemesRedisRetriever,
      oldSetObservedTotalScore: Double,
      oldSetExpectedTotalScore: Double,
      newSetObservedTotalScore: Double,
      newSetExpectedTotalScore: Double
      )) => {

      val newSetChiSquaredForTerm = newSetMorphemesRetriever.chiSquaredForTerm(term, newObservedScore, newSetExpectedTotalScore, newSetObservedTotalScore)
      val oldSetChiSquaredForTerm = oldSetMorphemesRetriever.chiSquaredForTerm(term, oldSetExpectedTotalScore, oldSetObservedTotalScore)

      if (newSetChiSquaredForTerm > oldSetChiSquaredForTerm) {
        cacheChiSquaredDiff(trendsCacheKey, term, (newSetChiSquaredForTerm - oldSetChiSquaredForTerm))
      }
      sender ! true
    }

    case _ => println("MorphemesTrendDetectActor says 'huh???'")
  }

  def cacheChiSquaredDiff(cacheKey: RedisKey, term: String, difference: Double) = {
    redisPool.withClient { redis =>
      {
        redis.pipeline { pipe =>
          pipe.zincrby(cacheKey.redisKey, difference, term)
          pipe.pexpire(cacheKey.redisKey, RichInt(15).minute.millis.toInt)
        }
      }
    }
  }

}