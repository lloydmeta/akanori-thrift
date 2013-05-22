package org.beachape.analyze
import com.redis._

case class MorphemesRedisTracker(morphemeList: List[Morpheme], redis: RedisClient, redisKey: String) extends RedisHelper {

  def storeAllInRedis = {
    for (morpheme <- morphemeList) {
      storeInRedis(morpheme)
    }
  }

  private def storeInRedis(morpheme: Morpheme) = {
    redis.zincrby(redisKey, 1, morpheme.surface)
    redis.zincrby(redisKey, 1, zSetTotalScoreKey)
  }

}