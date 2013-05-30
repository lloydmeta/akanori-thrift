package org.beachape.actors
import akka.actor.Actor
import akka.event.Logging
import com.redis._
import org.beachape.analyze.MorphemesRedisRetriever

class MorphemeRedisRetrieverActor(val redisPool: RedisClientPool) extends Actor {

  def receive = {

    case (RedisKeySet(RedisKey(expectedKey), RedisKey(observedKey)), minOccurence: Double) => {
      val morphemesRetriever = MorphemesRedisRetriever(redisPool, expectedKey, observedKey, minOccurence)
      sender ! RedisKey(morphemesRetriever.retrieveStorageKey)
    }

    case List('retrieveChiChi, RedisKeySet(RedisKey(expectedKey), RedisKey(observedKey)), minOccurence: Double, minLength: Int, maxLength: Int, top: Int) => {
      println("Retreiving ChiSquared")
      println("*********************")

      val morphemesRetriever = MorphemesRedisRetriever(redisPool, expectedKey, observedKey, minScore = Double.NegativeInfinity)

      val listOfReverseSortedTermsAndScores = for ((term, chiScore) <- morphemesRetriever.byChiSquared().filter(x => x._1.length >= minLength && x._1.length <= maxLength).take(top)) yield {
        (term, chiScore)
      }

      sender ! listOfReverseSortedTermsAndScores
    }

    case _ => println("huh?")
  }

}