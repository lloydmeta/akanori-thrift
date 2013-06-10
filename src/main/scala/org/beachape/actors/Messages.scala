package org.beachape.actors
// Message definitions

sealed case class RedisKey(redisKey: String)
sealed case class RedisKeySet(expectedKey: RedisKey, observedKey: RedisKey)

sealed case class UnixTime(time: Int)
sealed case class UnixTimeSpan(start: UnixTime, end: UnixTime)

sealed case class StoreString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)