package org.beachape.analyze
import com.redis._
import com.redis.RedisClient._
import scala.math.pow

case class MorphemesRedisRetriever(redisPool: RedisClientPool, redisKeyOlder: String, redisKeyNewer: String, minScore: Double = 10) extends ChiSquare with RedisHelper {

  def byChiSquaredReversed = {
    byChiSquared.reverse
  }

  def storeChiSquared = {
    val storageKey = f"MorphemesChiSquared:$redisKeyOlder%s-$redisKeyNewer%s"
    for ((term, chiSquaredScore) <- byChiSquared) {
      redisPool.withClient { redis =>
        redis.zincrby(storageKey, chiSquaredScore, term)
        redis.zincrby(storageKey, chiSquaredScore, zSetTotalScoreKey)
      }
    }
    storageKey
  }

  def byChiSquared: List[(String, Double)] = {
    val oldSetTotalScore = redisPool.withClient { redis =>
      redis.zscore(redisKeyOlder, zSetTotalScoreKey) match {
        case None => 0
        case Some(x) => x
      }
    }

    val newSetTotalScore = redisPool.withClient { redis =>
      redis.zscore(redisKeyNewer, zSetTotalScoreKey) match {
        case None => 0
        case Some(x) => x
      }
    }

    val morphemeChiSquaredList: List[(String, Double)] = {
      for ((term, newForTermScore) <- newTermsWithScoresList) yield {
        val oldScoreForTerm = redisPool.withClient { redis =>
          redis.zscore(redisKeyOlder, term) match {
            case Some(y) => y
            case _ => 1
          }
        }
        if (newForTermScore > oldScoreForTerm)
          (term, calculateChiSquaredForTerm(oldScoreForTerm, newForTermScore, oldSetTotalScore, newSetTotalScore))
        else
          (term, -55378008.0)
      }
    }

    morphemeChiSquaredList.filter(_._2 != -55378008.0).sortBy(_._2)
  }

  def newTermsWithScoresList: List[(String, Double)] = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(redisKeyNewer, minScore, limit = None, sortAs = DESC) match {
        case Some(x: List[(String, Double)]) => x.filter(_._1 != zSetTotalScoreKey)
        case _ => Nil
      }
    }
  }

}