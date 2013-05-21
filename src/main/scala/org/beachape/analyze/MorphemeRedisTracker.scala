package org.beachape.analyze
import com.redis._

case class MorphemesRedisTracker(morphemeList: List[Morpheme], redis: RedisClient, redisKey: String) {

  def storeAllInRedis = {
    for (morpheme <- morphemeList)
      storeInRedis(morpheme)
  }

  private def storeInRedis(morpheme: Morpheme) = {
    redis.zincrby(redisKey, 1, morpheme.surface)
  }

}