package org.beachape.actors

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

trait RedisStorageHelper extends StringToStorableStringHelper {
  val redisPool: RedisClientPool

  def storedStringsSetKey = "trends:storedStrings"
  def defaultTrendCacheKey = "default:TrendCacheKey"
  def customTrendCacheKey = "default:customTrendCacheKey"
  def customTrendCacheKeyEndingNow = "default:customTrendCacheKeyEndingNow"

  def cachedKeyExists(key: RedisKey): Boolean = {
    redisPool.withClient { redis =>
      redis.exists(key.redisKey)
    }
  }

  def setExpiryOnRedisKey(key: RedisKey, expiryInSeconds: Int) {
    redisPool.withClient { redis =>
      redis.pexpire(key.redisKey, RichInt(expiryInSeconds).seconds.millis.toInt)
    }
  }

  def deleteKey(key: RedisKey) {
    redisPool.withClient { redis =>
      redis.del(key.redisKey)
    }
  }

}