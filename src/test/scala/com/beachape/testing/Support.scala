package com.beachape.testing

import com.beachape.actors.RedisKey
import com.beachape.actors.RedisKeySet
import com.beachape.helpers.RedisStorageHelper

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
    val unixStartTime = 478000
    val unixEndTime = 1104400
    val span = 10800

    val timeStampsToStringFrequencyMap = Map(
      //oldExpectedStrings
      (unixStartTime + 1) -> Map(
        "笹子" -> 2,
        "トンネル" -> 1,
        "設計" -> 2,
        "見落とし" -> 2,
        "ーーー" -> 3,
        "殺す" -> 3),
      //oldObservedStrings
      (unixStartTime + span + 1) -> Map(
        "笹子" -> 3,
        "トンネル" -> 4,
        "設計" -> 3,
        "見落とし" -> 1,
        "ーーー" -> 3,
        "殺す" -> 9),
      //newExpectededStrings
      (unixEndTime - 2 * span + 1) -> Map(
        "笹子" -> 2,
        "トンネル" -> 1,
        "設計" -> 4,
        "見落とし" -> 5,
        "坂本" -> 2,
        "ーーー" -> 2,
        "殺す" -> 1),
      //newObservedStrings
      (unixEndTime - span + 1) -> Map(
        "笹子" -> 4,
        "トンネル" -> 6,
        "設計" -> 4,
        "見落とし" -> 9,
        "坂本" -> 10,
        "ーーー" -> 6,
        "殺す" -> 9))

    for ((unixTime, frequencyMap) <- timeStampsToStringFrequencyMap) {
      for ((term, frequency) <- frequencyMap) {
        for (i <- (0 to (frequency - 1))) {
          val uniquishTimestamp = unixTime + i
          val storableString = stringToSetStorableString(term, f"system$i%d", uniquishTimestamp)
          redisPool.withClient { redis =>
            redis.zadd(storedStringsSetKey, uniquishTimestamp, storableString)
          }
        }
      }
    }

    Map(
      'unixStartTime -> unixStartTime,
      'unixEndTime -> unixEndTime,
      'span -> span)
  }

  def zcardOfRedisKey(key: String) = {
    redisPool.withClient(redis =>
      redis.zcard(key)) match {
      case Some(x: Long) => x.toInt
      case _ => 0
    }
  }

  val validTrendNoFiltering = List(
    ("殺す", 14.000470143155322),
    ("坂本", 7.072925925925933),
    ("見落とし", 4.973760416666666),
    ("笹子", 0.5673898467981636),
    ("ーー", 0.005333333333333368))

  val validTrendDropBlacklisted = List(
    ("見落とし", 18.247894967682377),
    ("殺す", 14.508305069379858),
    ("坂本", 3.9175442443828867),
    ("笹子", 0.9371766039803595))

  val validTrendOnlyWhitelisted = List(
    ("坂本", 14.43038580246913),
    ("笹子", 1.1631186536635252),
    ("ーー", 0.1168055555555556))

  val validTrendWithMinOccurrence = List(
    ("殺す", 14.000470143155322),
    ("坂本", 7.072925925925933),
    ("見落とし", 4.973760416666666),
    ("ーー", 0.005333333333333368))

  val validTrendWithMinLength = List(
    ("見落とし", 4.973760416666666))

  val validTrendWithMaxLength = List(
    ("殺す", 14.000470143155322),
    ("坂本", 7.072925925925933),
    ("笹子", 0.5673898467981636),
    ("ーー", 0.005333333333333368))

  val validTrendWithTop = List(
    ("殺す", 14.000470143155322),
    ("坂本", 7.072925925925933))

  def roughRound(double: Double, precision: Double) = double - double % precision

}