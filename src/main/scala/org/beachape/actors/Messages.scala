package org.beachape.actors
// Message definitions

sealed case class RedisKey(redisKey: String)
sealed case class RedisKeySet(expectedKey: RedisKey, observedKey: RedisKey)

sealed case class UnixTime(time: Int)
sealed case class UnixTimeSpan(start: UnixTime, end: UnixTime)

sealed case class StoreString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)

sealed case class GenerateDefaultTrends

sealed case class GenerateAndCacheTrendsFor(
  redisCacheKey: RedisKey,
  unixEndAtTime: Int,
  spanInSeconds: Int,
  minOccurrence: Double,
  minLength: Int,
  maxLength: Int,
  top: Int,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)

sealed case class GenerateMorphemesFor(
  unixEndAtTime: Int,
  spanInSeconds: Int,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)

sealed case class GenerateMorphemesForSpan(
  unixTimeSpan: UnixTimeSpan,
  dropBlacklisted: Boolean,
  onlyWhitelisted: Boolean)