package org.beachape.analyze

import com.redis._
import com.redis.RedisClient._
import scala.math.pow
import com.github.nscala_time.time.Imports._

case class MorphemesRedisRetriever(redisPool: RedisClientPool, redisKeyOlder: String, redisKeyNewer: String, minScore: Double = 10) extends ChiSquare with RedisHelper {

  def retrieveStorageKey = {
    if (zCardExists(storageKey)) {
      storageKey
    } else {
      generateAndStoreChiSquared
      storageKey
    }
  }

  def byChiSquared(limit: Option[(Int, Int)] = None, sortAs: SortOrder = DESC) = {
    if (zCardExists(storageKey)) {
      termsWithScoresList(storageKey, limit = limit, sort = sortAs)
    } else {
      generateAndStoreChiSquared
      termsWithScoresList(storageKey, limit = limit, sort = sortAs)
    }
  }

  def termsWithScoresList(redisKey: String, min: Double = Double.NegativeInfinity, limit: Option[(Int, Int)] = None, sort: SortOrder = DESC) = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(redisKey, min, limit = limit, sortAs = sort) match {
        case Some(x: List[(String, Double)]) => x.filter(_._1 != zSetTotalScoreKey)
        case _ => Nil
      }
    }
  }

  def newTermsWithScoresList: List[(String, Double)] = {
    termsWithScoresList(redisKeyNewer, min = minScore, limit = None, sort = DESC)
  }

  def newTermsWithScoresListWithLimit(limitDesired: Option[(Int, Int)] = Some(0, 50)): List[(String, Double)] = {
    termsWithScoresList(redisKeyNewer, min = minScore, limit = limitDesired, sort = DESC)
  }

  def forEachPageOfObservedTermsWithScores[A](pageCount: Int = 300)(callBack: List[(String, Double)] => A): List[A] = {
    val newObservedSetCard = observedZCard
    val offSets = 0 to newObservedSetCard by pageCount // Generate range to page over the new set

    (for (offSet <- offSets) yield {
      val morphemesWithScoresAtOffSet = newTermsWithScoresListWithLimit(Some(offSet, pageCount))
      callBack(morphemesWithScoresAtOffSet)
    })(collection.breakOut)
  }

  def generateAndStoreChiSquared: String = {
    val oldSetTotalScore = totalMorphemesScoreAtSet(redisKeyOlder)
    val newSetCard = zCard(redisKeyNewer)
    val newSetTotalScore = totalMorphemesScoreAtSet(redisKeyNewer)

    val count = 300 // How many to retrieve at once
    val offSets = 0 to newSetCard by count // Generate range to page over the new set

    offSets foreach { offSet =>
      val morphemeSquaredListForOffset = for ((term, newScoreForTerm) <- newTermsWithScoresListWithLimit(Some(offSet, count))) yield {
        val oldScoreForTerm = getOldScoreForTerm(term)
        if (newScoreForTerm > oldScoreForTerm && newScoreForTerm != 0) {
          (term, calculateChiSquaredForTerm(oldScoreForTerm, newScoreForTerm, oldSetTotalScore, newSetTotalScore))
        } else {
          (term, Double.PositiveInfinity)
        }
      }
      storeScoresInZSet(morphemeSquaredListForOffset.filter(_._2 != Double.PositiveInfinity))
    }

    setExpiryOnRedisKey(storageKey, 60 * 5)
    storageKey
  }

  def storageKey = f"MorphemesChiSquared:$redisKeyOlder%s-$redisKeyNewer%s-minScore$minScore%f"

  def storeScoresInZSet(morphemeSquaredList: List[(String, Double)]) = {
    for ((term: String, chiSquaredScore: Double) <- morphemeSquaredList) {
      redisPool.withClient { redis =>
        redis.pipeline { p =>
          p.zincrby(storageKey, chiSquaredScore, term)
          p.zincrby(storageKey, chiSquaredScore, zSetTotalScoreKey)
        }
      }
    }
  }

  def getScoreForTerm(redisKey: String, term: String) = {
    redisPool.withClient { redis =>
      redis.zscore(redisKey, term) match {
        case Some(y) => y
        case _ => 0
      }
    }
  }

  def getOldScoreForTerm(term: String) = {
    getScoreForTerm(redisKeyOlder, term)
  }

  def getNewScoreForTerm(term: String) = {
    getScoreForTerm(redisKeyNewer, term)
  }

  private def totalMorphemesScoreAtSet(redisKey: String) = {
    redisPool.withClient { redis =>
      redis.zscore(redisKey, zSetTotalScoreKey) match {
        case Some(x) => x
        case _ => 0
      }
    }
  }

  def totalExpectedSetMorphemesScore = {
    totalMorphemesScoreAtSet(redisKeyOlder)
  }

  def totalObservedSetMorphemesScore = {
    totalMorphemesScoreAtSet(redisKeyNewer)
  }

  def zCard(redisKey: String) = {
    redisPool.withClient { redis =>
      redis.zcard(redisKey) match {
        case Some(x: Long) => x.toInt
        case None => 0
      }
    }
  }

  def observedZCard = {
    zCard(redisKeyNewer)
  }

  def setExpiryOnRedisKey(key: String, expiryInSeconds: Int) = {
    redisPool.withClient { redis =>
      redis.pexpire(key, RichInt(expiryInSeconds).seconds.millis.toInt)
    }
  }

  def zCardExists(redisKey: String) = {
    redisPool.withClient { redis =>
      redis.exists(redisKey)
    }
  }

  def chiSquaredForTerm(term: String, observedScoreForTerm: Double, expectedSetTotalScore: Double, observedSetTotalScore: Double) = {
    val expectedScoreForTerm = getOldScoreForTerm(term)
    if (observedScoreForTerm > expectedScoreForTerm)
      calculateChiSquaredForTerm(expectedScoreForTerm, observedScoreForTerm, expectedSetTotalScore, observedSetTotalScore)
    else
      0
  }

  def chiSquaredForTerm(term: String, expectedSetTotalScore: Double, observedSetTotalScore: Double): Double = {
    val observedScoreForTerm = getNewScoreForTerm(term)
    chiSquaredForTerm(term, observedScoreForTerm, expectedSetTotalScore, observedSetTotalScore)
  }

}