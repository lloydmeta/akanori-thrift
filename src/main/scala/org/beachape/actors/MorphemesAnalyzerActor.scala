package org.beachape.actors

import akka.actor.Actor
import akka.event.Logging
import com.redis._
import org.beachape.analyze.{ Morpheme, RedisHelper }

class MorphemesAnalyzerActor(val redisPool: RedisClientPool) extends Actor with RedisHelper with RedisStorageHelper {

  def receive = {

    case List('dumpMorphemesToRedis, redisKey: RedisKey, line: String, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) => {
      val morphemes = Morpheme.stringToMorphemes(line, dropBlacklisted, onlyWhitelisted)
      storeAllInRedis(morphemes, redisKey)
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