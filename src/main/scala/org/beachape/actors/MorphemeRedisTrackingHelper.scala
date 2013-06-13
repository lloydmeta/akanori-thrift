package org.beachape.actors

import com.redis.RedisClientPool
import org.beachape.analyze.Morpheme

/**
 * Tracker of morphemes and the number of times they occurred
 * in a list
 *
 * Classes that extend this must have redisPool defined
 */
trait MorphemesRedisTrackingHelper extends MorphemeScoreRedisHelper {

  val redisPool: RedisClientPool

  /**
   * Stores a set of morphemes into a sorted set with each morpheme's
   * surface (string value) as a member and times they showed up as the score
   *
   * @param morphemeList list of morphemes
   * @redisKey key to store the sorted set at
   */
  def storeAllInRedis(morphemeList: List[Morpheme], redisKey: RedisKey) {
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