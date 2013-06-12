package org.beachape.actors

import org.beachape.analyze.Morpheme
import org.beachape.analyze.MorphemeScoreRedisHelper

import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

object MorphemesAnalyzerActor {
  def apply(redisPool: RedisClientPool) = Props(new MorphemesAnalyzerActor(redisPool))
}

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