package org.beachape.analyze

import com.redis.RedisClientPool


/**
 * Tracker of morphemes and the number of times they occurred
 * in a list
 *
 * Each morpheme's .surface (the string value) is used as a member
 * of the Redis Sorted Set and the member's score is incremented
 * every time it shows up in the list
 *
 * @constructor create a new MorphemesRedisTracker instance
 * @param morphemeList list of morphemes to dump
 * @param redisPool the RedisClientPool this retriever should use
 * @param redisKey the key used for morpheme occurrence tracking
 */
case class MorphemesRedisTracker(
  morphemeList: List[Morpheme],
  redisPool: RedisClientPool,
  redisKey: String) extends MorphemeScoreRedisHelper {

  /**
   * Stores all the morphemes and the times they occurred
   * in the list of morphemes passed in during instantiation
   * into the sorted set located in the key provided during instantiation
   */
  def storeAllInRedis {
    for (morpheme <- morphemeList) {
      storeInRedis(morpheme)
    }
  }

  private def storeInRedis(morpheme: Morpheme) {
    redisPool.withClient {
      redis =>
        {
          redis.zincrby(redisKey, 1, morpheme.surface)
          redis.zincrby(redisKey, 1, zSetTotalScoreKey)
        }
    }

  }

}