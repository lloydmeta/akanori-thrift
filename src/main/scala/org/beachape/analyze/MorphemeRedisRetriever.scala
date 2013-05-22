package org.beachape.analyze
import com.redis._
import com.redis.RedisClient._
import scala.math.pow

case class MorphemesRedisRetriever(redis: RedisClient, redisKeyOlder: String, redisKeyNewer: String) {

  def byChiSquaredReversed = {
    byChiSquared.reverse
  }

  def byChiSquared: List[(String, Double)] = {
    val oldSetTotalScore = redis.zscore(redisKeyOlder, f"$redisKeyOlder%s__akanori_score_counter__") match {
      case None => 0
      case Some(x) => x
    }

    val newSetTotalScore = redis.zscore(redisKeyNewer, f"$redisKeyNewer%s__akanori_score_counter__") match {
      case None => 0
      case Some(x) => x
    }

    val morphemeChiSquaredList: List[(String, Double)] = {
      newTermsWithScoresList map { x =>
        (x._1, calculateChiSquaredForTerm(x._1, x._2, oldSetTotalScore, newSetTotalScore))
      }
    }

    morphemeChiSquaredList.sortBy(_._2)
  }

  def newTermsWithScoresList: List[(String, Double)] = {
    redis.zrangebyscoreWithScore(redisKeyNewer, limit = None, sortAs = DESC) match {
      case Some(x: List[(String, Double)]) => x
      case _ => Nil
    }
  }

  def calculateChiSquaredForTerm(term: String, termScore: Double, oldSetTotalScore: Double, newSetTotalScore: Double): Double = {
    val oldScoreForTerm = redis.zscore(redisKeyOlder, term) match {
      case Some(x) if x > 0 => x
      case _ => 1
    }

    // Calculate frequencies
    val observedTermFrequency = termScore / newSetTotalScore
    val expectedTermFrequency = oldScoreForTerm / oldSetTotalScore
    val otherObservedFrequency = (newSetTotalScore - termScore) / newSetTotalScore
    val otherExpectedFrequency = (oldSetTotalScore - oldScoreForTerm) / oldSetTotalScore

    val normalizer = List(newSetTotalScore, oldSetTotalScore).max

    val termChiSquaredPart = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency, normalizer)
    val otherChiSquaredPart = calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency, normalizer)

    termChiSquaredPart + otherChiSquaredPart
  }

  private def calculateChiSquaredPart(expectedFrequency: Double, observedFrequency: Double, normalizer: Double) = {
    pow(((observedFrequency * normalizer - expectedFrequency * normalizer).abs - 0.5), 2) / (normalizer * expectedFrequency)
  }

}