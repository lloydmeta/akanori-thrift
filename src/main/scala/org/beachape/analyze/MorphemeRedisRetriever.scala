package org.beachape.analyze
import com.redis._
import com.redis.RedisClient._
import scala.math.pow

case class MorphemesRedisRetriever(redis: RedisClient, redisKeyOlder: String, redisKeyNewer: String) {

  def byChiSquaredReversed = {
    byChiSquared.reverse
  }

  def byChiSquared: List[(String, Double)] = {
    val morphemeChiSquaredList: List[(String, Double)] = {
      newTermsWithScoresList map {x =>
        (x._1, calculateChiSquaredForTerm(x._1, x._2))
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

  def calculateChiSquaredForTerm(term: String, termScore: Double): Double = {
     val oldScoreForTerm = redis.zscore(redisKeyOlder, term) match {
      case None => 0
      case Some(x) => x
    }

    pow(((termScore - oldScoreForTerm).abs - 0.5), 2) / (oldScoreForTerm + 1)
  }

}