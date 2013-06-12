package org.beachape.actors

import org.beachape.analyze.Morpheme
import org.beachape.analyze.MorphemeScoreRedisHelper

import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

/** Factory for Props used to instantiate [[org.beachape.actors.MorphemesAnalyzerActor]] */
object MorphemesAnalyzerActor {

  /**
   * Returns the Props required to spawn an instance of MorphemesAnalyzerActor
   *
   * @params redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new MorphemesAnalyzerActor(redisPool))
}

/**
 * Actor that receives phrases, dropBlacklisted
 * and onlyWhitelisted options, and a RedisKey to
 * store results in, then counts the number
 * of morphemes in the phrase and stores them in the key
 * as a zSet
 *
 * Should be instantiated via the factory method in
 * the companion object above
 */
class MorphemesAnalyzerActor(val redisPool: RedisClientPool) extends Actor
  with MorphemeScoreRedisHelper
  with RedisStorageHelper {

  def receive = {

    case message: AnalyseAndStoreInRedisKey => {
      val morphemes = Morpheme.stringToMorphemesReverse(
        message.phrase,
        message.dropBlacklisted,
        message.onlyWhitelisted)
      storeAllInRedis(morphemes, message.redisKey)
      sender ! true
    }

    case _ => println("MorphemesAnalyzerActor says 'huh? '")
  }

  private def storeAllInRedis(morphemeList: List[Morpheme], redisKey: RedisKey) = {
    for (morpheme <- morphemeList) {
      storeInRedis(morpheme, redisKey)
    }
  }

  private def storeInRedis(morpheme: Morpheme, redisKey: RedisKey) = {
    redisPool.withClient {
      redis =>
        {
          redis.zincrby(redisKey.redisKey, 1, morpheme.surface)
          redis.zincrby(redisKey.redisKey, 1, zSetTotalScoreKey)
        }
    }
  }

}