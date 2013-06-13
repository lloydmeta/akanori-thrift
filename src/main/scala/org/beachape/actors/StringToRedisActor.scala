package org.beachape.actors

import com.github.nscala_time.time.Imports.DateTime
import com.github.nscala_time.time.Imports.RichDateTime
import com.github.nscala_time.time.Imports.RichInt
import com.github.nscala_time.time.Imports.RichReadableInstant
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props

/**
 * Companion object that houses the factory apply
 * method that returns the Props required to instantiate
 * a [[org.beachape.actors.StringToRedisActor]]
 */
object StringToRedisActor {

  /**
   * Returns the Props required to spawn an instance of StringToRedisActor
   *
   * @param redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new StringToRedisActor(redisPool))
}

/**
 * Actor that receives StoreString messages (see [[org.beachape.actors.Messages]]),
 * which contains a string to store, a unixCreatedAtTime, and weeksAgoDataToExpire
 * and stores the string in the Redis string sorted set. Should be instantiated via the
 * Props returned from the companion object's apply method.
 *
 * The helper function storedStringsSetKey is used to determine the key at which the sorted
 * set should be stored, the unixCreatedAtTime is used as the score of the member
 * being stored, and weeksAgoDataToExpire is used to trim the sorted set on every
 * attempt to store a string. Before storing the string, it is passed through
 * the stringToSetStorableString helper function to make it storable (unique within a given
 * time frame). This means that the storedStringToString helper must be called upon
 * retrieving to clean up strings that are retrieved from this set.
 */
class StringToRedisActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  def receive = {

    case message: StoreString => {
      storeString(message.stringToStore, message.unixCreatedAtTime, message.weeksAgoDataToExpire)
    }

    case _ => println("StringToRedisActor says 'huh?'")
  }

  private def storeString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int) {
    val storableString = stringToSetStorableString(stringToStore, unixCreatedAtTime)
    redisPool.withClient {
      redis =>
        {
          redis.zremrangebyscore(storedStringsSetKey, Double.NegativeInfinity, oldestScoreToKeep(weeksAgoDataToExpire))
          redis.zadd(storedStringsSetKey, unixCreatedAtTime, storableString)
        }
    }
  }

  private def oldestScoreToKeep(weeksAgoDataToExpire: Int = 2): Double = {
    ((DateTime.now - weeksAgoDataToExpire.weeks).millis / 1000).toDouble
  }

}