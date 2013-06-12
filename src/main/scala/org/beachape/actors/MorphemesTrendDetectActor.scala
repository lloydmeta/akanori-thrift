package org.beachape.actors

import scala.concurrent.duration.DurationInt

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.util.Timeout

object MorphemesTrendDetectActor {
  def apply(redisPool: RedisClientPool) = Props(new MorphemesTrendDetectActor(redisPool))
}

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