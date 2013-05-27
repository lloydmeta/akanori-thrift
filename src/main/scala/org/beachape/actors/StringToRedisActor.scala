package org.beachape.actors

import akka.actor.Actor
import akka.event.Logging
import com.redis._
import com.github.nscala_time.time.Imports._

class StringToRedisActor(redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  def receive = {

    case List('storeString, (stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)) => {
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