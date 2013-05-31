package org.beachape.testing

import com.redis._
import org.beachape.actors._

trait Support {

  val redisPool: RedisClientPool

  def dumpMorphemesToRedis: (RedisKeySet, RedisKeySet) = {
    val morphemesCountMapOldExpected = Map("a" -> 3, "b" -> 4, "c" -> 5, "d" -> 5, "e" -> 4)
    val morphemesCountMapOldObserved = Map("a" -> 3, "b" -> 6, "c" -> 10, "d" -> 2, "e" -> 5)
    val morphemesCountMapNewExpected = Map("a" -> 4, "b" -> 2, "c" -> 12, "d" -> 9, "e" -> 1)
    val morphemesCountMapNewObserved = Map("a" -> 4, "b" -> 10, "c" -> 13, "d" -> 12)

    val zSetTotalScoreMember = "{__akanori_score_counter__}"
    val oldExpectedRedisKey = "oldExpectedRedisKey"
    val oldObservedRedisKey = "oldObservedRedisKey"
    val newExpectedRedisKey = "newExpectedRedisKey"
    val newObservedRedisKey = "newObservedRedisKey"

    val redisKeyToMorphemesCountMap = Map(
      oldExpectedRedisKey -> morphemesCountMapOldExpected,
      oldObservedRedisKey -> morphemesCountMapOldObserved,
      newExpectedRedisKey -> morphemesCountMapNewExpected,
      newObservedRedisKey -> morphemesCountMapNewObserved)

    for ((key, morphemesScoreMap) <- redisKeyToMorphemesCountMap) {
      redisPool.withClient { redis =>
        redis.pipeline(pipe =>
          for ((morpheme, score) <- morphemesScoreMap) {
            pipe.zincrby(key, score, morpheme)
            pipe.zincrby(key, score, zSetTotalScoreMember)
          })
      }
    }
    (
      RedisKeySet(
        RedisKey(oldExpectedRedisKey),
        RedisKey(oldObservedRedisKey)
      ),
      RedisKeySet(
        RedisKey(newExpectedRedisKey),
        RedisKey(newObservedRedisKey)
      )
    )

  }

  def zcardOfRedisKey(key: String) = {
    redisPool.withClient(redis =>
      redis.zcard(key)) match {
      case Some(x: Long) => x.toInt
      case _ => 0
    }
  }

  def roughRound(double: Double, precision: Double) = double - double % precision

}