package org.beachape.analyze

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClient.DESC
import com.redis.RedisClient.SortOrder
import com.redis.RedisClientPool

case class MorphemesRedisRetriever(redisPool: RedisClientPool, redisKeyExpected: String, redisKeyObserved: String, minScore: Double = 10) extends ChiSquare with RedisHelper {

  def forEachPageOfObservedTermsWithScores[A](pageCount: Int = 300)(callBack: List[(String, Double)] => A): List[A] = {
    val newObservedSetCard = observedZCard
    val offSets = 0 to newObservedSetCard by pageCount // Generate range to page over the new set

    (for (offSet <- offSets) yield {
      val morphemesWithScoresAtOffSet = newTermsWithScoresListWithLimit(Some(offSet, pageCount))
      callBack(morphemesWithScoresAtOffSet)
    })(collection.breakOut)
  }

  def getExpectedScoreForTerm(term: String) = {
    getScoreForTerm(redisKeyExpected, term)
  }

  def getObservedScoreForTerm(term: String) = {
    getScoreForTerm(redisKeyObserved, term)
  }

  def totalExpectedSetMorphemesScore = {
    totalMorphemesScoreAtSet(redisKeyExpected)
  }

  def totalObservedSetMorphemesScore = {
    totalMorphemesScoreAtSet(redisKeyObserved)
  }

  def observedZCard = {
    zCard(redisKeyObserved)
  }

  def chiSquaredForTerm(term: String, observedScoreForTerm: Double, expectedSetTotalScore: Double, observedSetTotalScore: Double) = {
    val expectedScoreForTerm = getExpectedScoreForTerm(term)
    if (observedScoreForTerm > expectedScoreForTerm)
      calculateChiSquaredForTerm(expectedScoreForTerm, observedScoreForTerm, expectedSetTotalScore, observedSetTotalScore)
    else
      0
  }

  def chiSquaredForTerm(term: String, expectedSetTotalScore: Double, observedSetTotalScore: Double): Double = {
    val observedScoreForTerm = getObservedScoreForTerm(term)
    chiSquaredForTerm(term, observedScoreForTerm, expectedSetTotalScore, observedSetTotalScore)
  }

  private def termsWithScoresList(redisKey: String, min: Double = Double.NegativeInfinity, limit: Option[(Int, Int)] = None, sort: SortOrder = DESC) = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(redisKey, min, limit = limit, sortAs = sort) match {
        case Some(x: List[(String, Double)]) => x.filter(_._1 != zSetTotalScoreKey)
        case _ => Nil
      }
    }
  }

  private def newTermsWithScoresListWithLimit(limitDesired: Option[(Int, Int)] = Some(0, 50)): List[(String, Double)] = {
    termsWithScoresList(redisKeyObserved, min = minScore, limit = limitDesired, sort = DESC)
  }

  private def zCard(redisKey: String) = {
    redisPool.withClient { redis =>
      redis.zcard(redisKey) match {
        case Some(x: Long) => x.toInt
        case None => 0
      }
    }
  }

  private def totalMorphemesScoreAtSet(redisKey: String) = {
    redisPool.withClient { redis =>
      redis.zscore(redisKey, zSetTotalScoreKey) match {
        case Some(x) => x
        case _ => 0
      }
    }
  }

  private def getScoreForTerm(redisKey: String, term: String) = {
    redisPool.withClient { redis =>
      redis.zscore(redisKey, term) match {
        case Some(y) => y
        case _ => 0
      }
    }
  }

}