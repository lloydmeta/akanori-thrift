package org.beachape.actors

import com.github.nscala_time.time.Imports.DateTime
import com.github.nscala_time.time.Imports.RichDateTime
import com.github.nscala_time.time.Imports.RichInt
import com.github.nscala_time.time.Imports.RichReadableInstant
import com.redis.RedisClientPool

import akka.actor.Actor

class StringToRedisActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  def receive = {

    case StoreString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int) => {
      storeString(stringToStore, unixCreatedAtTime, weeksAgoDataToExpire)
    }

    case _ => println("StringToRedisActor says 'huh?'")
  }

  def oldestScoreToKeep(weeksAgoDataToExpire: Int = 2) = {
    ((DateTime.now - weeksAgoDataToExpire.weeks).millis / 1000).toDouble
  }

  def storeString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int) = {
    val storableString = stringToSetStorableString(stringToStore, unixCreatedAtTime)
    redisPool.withClient {
      redis =>
        {
          redis.zremrangebyscore(storedStringsSetKey, Double.NegativeInfinity, oldestScoreToKeep(weeksAgoDataToExpire))
          redis.zadd(storedStringsSetKey, unixCreatedAtTime, storableString)
        }
    }
  }

}