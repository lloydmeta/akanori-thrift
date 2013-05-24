package org.beachape.actors
import akka.actor.Actor
import akka.event.Logging
import com.redis._
import org.beachape.analyze.MorphemesRedisRetriever

class MorphemeRedisRetrieverActor(redisPool: RedisClientPool) extends Actor {

  def receive = {
    case (RedisKeySet(RedisKey(expectedKey), RedisKey(observedKey)), minOccurence: Double) => {
      val morphemesRetriever = MorphemesRedisRetriever(redisPool, expectedKey, observedKey, minOccurence)
      sender ! RedisKey(morphemesRetriever.storeChiSquared)
    }
    case List('printChiChi, RedisKeySet(RedisKey(expectedKey), RedisKey(observedKey)), minOccurence: Double, minLength: Int, maxLength: Int, top: Int) => {
      println("ChiSquared")
      println("**********")
      val morphemesRetriever = MorphemesRedisRetriever(redisPool, expectedKey, observedKey, minScore = Double.NegativeInfinity)
      for ((term, chiScore) <- morphemesRetriever.byChiSquaredReversed.filter(x => x._1.length >= minLength && x._1.length <= maxLength).take(top))
        println(s"Term: [$term], χ² of χ² score $chiScore")
      sender ! 'allDone
    }

    case List('retrieveChiChi, RedisKeySet(RedisKey(expectedKey), RedisKey(observedKey)), minOccurence: Double, minLength: Int, maxLength: Int, top: Int) => {
      println("Retreiving ChiSquared")
      println("*********************")
      val morphemesRetriever = MorphemesRedisRetriever(redisPool, expectedKey, observedKey, minScore = Double.NegativeInfinity)

      val listOfReverseSortedTermsAndScores = for ((term, chiScore) <- morphemesRetriever.byChiSquaredReversed.filter(x => x._1.length >= minLength && x._1.length <= maxLength).take(top)) yield {
        (term, chiScore)
      }

      sender ! listOfReverseSortedTermsAndScores
    }

    case _ => println("huh?")
  }

}