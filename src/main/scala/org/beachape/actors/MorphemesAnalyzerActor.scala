package org.beachape.actors

import org.beachape.analyze.Morpheme
import org.beachape.helpers.MorphemesRedisTrackingHelper
import org.beachape.helpers.RedisStorageHelper

import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala

/** Companion object housing the factory for Props used to instantiate
 *  [[org.beachape.actors.MorphemesAnalyzerActor]] */
object MorphemesAnalyzerActor {

  /**
   * Returns the Props required to spawn an instance of MorphemesAnalyzerActor
   *
   * @param redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new MorphemesAnalyzerActor(redisPool))
}

/**
 * Actor that receives phrases, dropBlacklisted
 * and onlyWhitelisted options, and a RedisKey to
 * store results in, then counts the number
 * of morphemes in the phrase and stores them in the key
 * as a sorted set
 *
 * Should be instantiated via the factory method in
 * the companion object above
 */
class MorphemesAnalyzerActor(val redisPool: RedisClientPool) extends Actor
  with MorphemesRedisTrackingHelper
  with RedisStorageHelper {

  def receive = {

    case message: AnalyseAndStoreInRedisKey => {
      val morphemes = Morpheme.stringToMorphemesReverse(
        message.phrase,
        message.dropBlacklisted,
        message.onlyWhitelisted)
      storeAllInRedis(morphemes, message.redisKey)
      sender ! true
    }

    case _ => println("MorphemesAnalyzerActor says 'huh? '")
  }

}