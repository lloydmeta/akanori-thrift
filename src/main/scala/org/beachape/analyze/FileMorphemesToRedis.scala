package org.beachape.analyze

import com.redis._
import scala.io.Source

case class FileMorphemesToRedis(path: String, redisPool: RedisClientPool, redisKey: String, dropBlacklisted: Boolean = true, onlyWhitelisted: Boolean = false) {

  def dumpToRedis = {
    val morphemes = Source.fromFile(path).getLines().flatMap(line =>
      Morpheme.stringToMorphemes(line, dropBlacklisted, onlyWhitelisted)
    ).toList

    val morphemeTracker = MorphemesRedisTracker(morphemes, redisPool, redisKey)
    morphemeTracker.storeAllInRedis
  }

}