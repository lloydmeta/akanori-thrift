package org.beachape.actors
// Message definitions

sealed case class RedisKey(redisKey: String)
sealed case class RedisKeySet(expectedKey: RedisKey, observedKey: RedisKey)

sealed case class FilePath(filePath: String)
sealed case class FilePathSet(expected: FilePath, observed: FilePath)
sealed case class FullFilePathSet(older: FilePathSet, newer: FilePathSet)

sealed case class UnixTime(time: Int)
sealed case class UnixTimeSpan(start: UnixTime, end: UnixTime)