package org.beachape.actors

import com.github.nscala_time.time.Imports.DateTime
import com.github.nscala_time.time.Imports.RichDateTime
import com.github.nscala_time.time.Imports.RichInt
import com.github.nscala_time.time.Imports.RichReadableInstant
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props

object StringToRedisActor {
  def apply(redisPool: RedisClientPool) = Props(new StringToRedisActor(redisPool))
}

class StringToRedisActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  def receive = {

    case message: StoreString => {
      storeString(message.stringToStore, message.unixCreatedAtTime, message.weeksAgoDataToExpire)
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