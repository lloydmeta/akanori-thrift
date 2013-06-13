package org.beachape.helpers

import org.beachape.actors.RedisKey

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

/** Provides helpers for RedisFunctions
 *
 *  Helpers for referencing Redis access keys and
 *  basic checking of whether a key exists in Redis,
 *  deletion of a key and setting the expiry of a key
 *
 *  Classes that mix in this trait need to have a
 *   - val redisPool defined
 *
 */
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
      redis.pexpire(key.redisKey, expiryInSeconds.seconds.millis.toInt)
    }
  }

  def deleteKey(key: RedisKey) {
    redisPool.withClient { redis =>
      redis.del(key.redisKey)
    }
  }

}