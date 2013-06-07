package org.beachape.analyze

import com.redis.RedisClientPool

case class MorphemesRedisTracker(morphemeList: List[Morpheme], redisPool: RedisClientPool, redisKey: String) extends MorphemeScoreRedisHelper {

  def storeAllInRedis = {
    for (morpheme <- morphemeList) {
      storeInRedis(morpheme)
    }
  }

  private def storeInRedis(morpheme: Morpheme) = {
    redisPool.withClient {
      redis =>
        {
          redis.zincrby(redisKey, 1, morpheme.surface)
          redis.zincrby(redisKey, 1, zSetTotalScoreKey)
        }
    }

  }

}