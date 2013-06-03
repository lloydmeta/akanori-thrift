package org.beachape.testing

import com.redis._
import org.beachape.actors._

trait Support extends RedisStorageHelper {

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
        RedisKey(oldObservedRedisKey)),
        RedisKeySet(
          RedisKey(newExpectedRedisKey),
          RedisKey(newObservedRedisKey)))

  }

  def dumpStringsToRedisStoredStringSet: Map[Symbol, Int] = {
    val unixStartTime = 996400
    val unixEndTime = 1086400
    val span = 1800

    val timeStampsToStringFrequencyMap = Map(
        //oldExpectedStrings
        (unixStartTime + 1) -> Map(
        "笹子" -> 2,
        "トンネル" -> 1,
        "設計" -> 2,
        "見落とし" -> 2),
        //oldObservedStrings
        (unixStartTime + span + 1) -> Map(
        "笹子" -> 3,
        "トンネル" -> 4,
        "設計" -> 3,
        "見落とし" -> 1),
        //newExpectededStrings
         (unixEndTime - span - 1) -> Map(
        "笹子" -> 2,
        "トンネル" -> 1,
        "設計" -> 4,
        "見落とし" -> 5),
        //newObservedStrings
        (unixEndTime - 1) -> Map(
        "笹子" -> 4,
        "トンネル" -> 6,
        "設計" -> 4,
        "見落とし" -> 9)
        )

    for ((unixTime, frequencyMap) <- timeStampsToStringFrequencyMap) {
      for ((term, frequency) <- frequencyMap) {
        for (i <- (0 to (frequency - 1))) {
          val uniquishTimestamp = unixTime + i
          val storableString = stringToSetStorableString(term, uniquishTimestamp)
          redisPool.withClient {redis =>
            redis.zadd(storedStringsSetKey, uniquishTimestamp, storableString)
          }
        }
      }
    }

    Map(
      'unixStartTime -> unixStartTime,
      'unixEndTime -> unixEndTime,
      'span -> span
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