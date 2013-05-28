package org.beachape.actors

import com.redis._
import com.github.nscala_time.time.Imports._

trait RedisStorageHelper {

  val redisPool: RedisClientPool

  def storedStringsSetKey = "trends:storedStrings"

  def defaultTrendCacheKey = "default:TrendCacheKey"
  def customTrendCacheKey = "default:customTrendCacheKey"
  def customTrendCacheKeyEndingNow = "default:customTrendCacheKeyEndingNow"

  def stringToSetStorableString(stringToStore: String, unixCreateTime: Int) = {
    val uniqueMarker = storedStringUniqueMarker(unixCreateTime)
    f"$uniqueMarker%s$stringToStore%s"
  }

  def storedStringToString(storedString: String) = {
    storedString.replaceFirst(f"$storedStringUniqueMarkerOpener%s.*$storedStringUniqueMarkerCloser%s", "")
  }

  def cachedKeyExists(key: RedisKey): Boolean = {
    redisPool.withClient { redis =>
      redis.exists(key.redisKey)
    }
  }

  def setExpiryOnRedisKey(key: RedisKey, expiryInSeconds: Int) = {
    redisPool.withClient { redis =>
      redis.pexpire(key.redisKey, RichInt(expiryInSeconds).seconds.millis.toInt)
    }
  }

  def deleteKey(key: RedisKey) = {
    redisPool.withClient { redis =>
      redis.del(key.redisKey)
    }
  }

  private def storedStringUniqueMarker(unixCreateTime: Int) = f"$storedStringUniqueMarkerOpener%s$unixCreateTime%d$storedStringUniqueMarkerCloser%s"
  private def storedStringUniqueMarkerOpener = "<--createdAt--"
  private def storedStringUniqueMarkerCloser = "--createdAt-->"

}