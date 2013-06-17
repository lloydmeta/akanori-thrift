package com.beachape.actors

import scala.concurrent.duration.DurationInt

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.Timeout

/**
 * Companion object that houses the factory for
 *  Props used to instantiate [[com.beachape.actors.MorphemesTrendDetectActor]]
 */
object MorphemesTrendDetectActor {

  /**
   * Returns the Props required to spawn an instance of MorphemesTrendDetectActor
   *
   * @param redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new MorphemesTrendDetectActor(redisPool))
}

/**
 * Actor that calculates the trendiness of a given term
 * and caches it.
 *
 * Message sent should contain the term, the term's new
 * observed occurrence, the total number of occurrences across
 * all terms in the new expected and observed sets as well
 * as the total number of occurrences in the old expected
 * and observed sets (see com.beachape.actors.Messages.CalculateAndStoreTrendiness)
 *
 * Should be instantiated via the factory method in
 * the companion object above
 */
class MorphemesTrendDetectActor(redisPool: RedisClientPool) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(DurationInt(600).seconds)

  def receive = {

    case message: CalculateAndStoreTrendiness => {

      val newSetChiSquaredForTerm = message.newSetMorphemesRetriever.chiSquaredForTerm(
        message.term,
        message.newObservedScore,
        message.newSetExpectedTotalScore,
        message.newSetObservedTotalScore)
      val oldSetChiSquaredForTerm = message.oldSetMorphemesRetriever.chiSquaredForTerm(
        message.term,
        message.oldSetExpectedTotalScore,
        message.oldSetObservedTotalScore)

      if (newSetChiSquaredForTerm > oldSetChiSquaredForTerm) {
        cacheChiSquaredDiff(message.trendsCacheKey, message.term, (newSetChiSquaredForTerm - oldSetChiSquaredForTerm))
      }
      sender ! true
    }

    case _ => println("MorphemesTrendDetectActor says 'huh???'")
  }

  private def cacheChiSquaredDiff(cacheKey: RedisKey, term: String, difference: Double) {
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