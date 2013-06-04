package org.beachape.actors

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import org.beachape.analyze.MorphemesRedisRetriever

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

class MorphemesTrendDetectActor(redisPool: RedisClientPool) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(DurationInt(600).seconds)

  def receive = {

    case List('calculateAndStoreTrendiness, (
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