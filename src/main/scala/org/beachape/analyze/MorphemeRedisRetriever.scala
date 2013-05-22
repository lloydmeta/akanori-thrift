package org.beachape.analyze
import com.redis._
import com.redis.RedisClient._
import scala.math.pow

case class MorphemesRedisRetriever(redis: RedisClient, redisKeyOlder: String, redisKeyNewer: String) extends ChiSquare with RedisHelper{

  def byChiSquaredReversed = {
    byChiSquared.reverse
  }

  def storeChiSquared = {
    val storageKey = f"MorphemesChiSquared:$redisKeyOlder%s-$redisKeyNewer%s"
    for ((term, chiSquaredScore) <- byChiSquared) {
      redis.zincrby(storageKey, chiSquaredScore, term)
      redis.zincrby(storageKey, chiSquaredScore, zSetTotalScoreKey)
    }
    storageKey
  }

  def byChiSquared: List[(String, Double)] = {
    val oldSetTotalScore = redis.zscore(redisKeyOlder, zSetTotalScoreKey) match {
      case None => 0
      case Some(x) => x
    }

    val newSetTotalScore = redis.zscore(redisKeyNewer, zSetTotalScoreKey) match {
      case None => 0
      case Some(x) => x
    }

    val morphemeChiSquaredList: List[(String, Double)] = {
      newTermsWithScoresList map { x =>
        val oldScoreForTerm = redis.zscore(redisKeyOlder, x._1) match {
          case Some(y) if y > 0 => y
          case _ => 1
        }
        (x._1, calculateChiSquaredForTerm(oldScoreForTerm, x._2, oldSetTotalScore, newSetTotalScore))
      }
    }

    morphemeChiSquaredList.sortBy(_._2)
  }

  def newTermsWithScoresList: List[(String, Double)] = {
    redis.zrangebyscoreWithScore(redisKeyNewer, limit = None, sortAs = DESC) match {
      case Some(x: List[(String, Double)]) => x.filter(_._1 != zSetTotalScoreKey)
      case _ => Nil
    }
  }

}