package org.beachape.analyze

import com.redis._
import scala.io.Source

case class FileMorphemesToRedis(path: String, redis: RedisClient, redisKey: String, dropBlacklisted: Boolean = true, onlyWhitelisted: Boolean = false) {

  def dumpToRedis = {
    val morphemes = Source.fromFile(path).getLines().flatMap(line =>
      Morpheme.stringToMorphemes(line, dropBlacklisted, onlyWhitelisted)
    ).toList
    for (m <- morphemes)
      println(m)
    val morphemeTracker = MorphemesRedisTracker(morphemes, redis, redisKey)
    morphemeTracker.storeAllInRedis
  }

}