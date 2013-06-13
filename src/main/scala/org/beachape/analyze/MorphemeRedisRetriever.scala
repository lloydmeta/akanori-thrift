package org.beachape.analyze

import com.redis.RedisClient.DESC
import com.redis.RedisClient.SortOrder
import com.redis.RedisClientPool
import org.beachape.actors.MorphemeScoreRedisHelper

/**
 * Retriever of morphemes and their respective counts
 *
 * @constructor create a new retriever
 * @param redisPool the RedisClientPool this retriever should use
 * @param redisKeyExpected a key pointing to a sorted set (expected)
 *  of morphemes with scores as the number of times they occurred
 * @param redisKeyObserved a key pointing to a a sorted set (observed)
 *  of morphemes with scores as the number of times they occurred
 */
case class MorphemesRedisRetriever(
  redisPool: RedisClientPool,
  redisKeyExpected: String,
  redisKeyObserved: String,
  minScore: Double = 10) extends ChiSquare with MorphemeScoreRedisHelper {

  /**
   * Returns a Some of a list of results of a callback function that
   * takes a Some of a list of morpheme and occurrence doubles as arguments.
   * It does this in pages
   *
   * @param pageCount number of items in a page (optional defaults to 300)
   * @param callBack a function that takes an Optional list of String Double pairs and produces
   *  a result
   */
  def mapEachPageOfObservedTermsWithScores[A](pageCount: Int = 300)(callBack: Option[List[(String, Double)]] => A): Option[List[A]] = {
    val newObservedSetCard = observedZCard
    val offSets = 0 to newObservedSetCard by pageCount // Generate range to page over the new set

    Some((for (offSet <- offSets) yield {
      val morphemesWithScoresAtOffSet = newTermsWithScoresListWithLimit(Some(offSet, pageCount))
      callBack(morphemesWithScoresAtOffSet)
    })(collection.breakOut))
  }

  /**
   * Returns the expected score (how many times did this term occur
   * in a past relevant timespan, based on the expected sorted set)
   * for a certain term
   *
   * @param term string for looking up a score
   */
  def getExpectedScoreForTerm(term: String): Double = {
    getScoreForTerm(redisKeyExpected, term)
  }

  /**
   * Returns the observed score (how many times did this term occur
   * in the current timespan of interest, based on the observed sorted set)
   * for a certain term
   *
   * @param term string for looking up a score
   */
  def getObservedScoreForTerm(term: String): Double = {
    getScoreForTerm(redisKeyObserved, term)
  }

  /**
   * Returns the total score of all morphemes in the expected
   * sorted Set.
   *
   * Could be looked upon as the total number of
   * times all morphemes showed up within the period
   * that the set was created for
   */
  def totalExpectedSetMorphemesScore: Double = {
    totalMorphemesScoreAtSet(redisKeyExpected)
  }

  /**
   * Returns the total score of all morphemes in the observed
   * sorted Set.
   *
   * Could be looked upon as the total number of
   * times all morphemes showed up within the period
   * that the set was created for
   */
  def totalObservedSetMorphemesScore: Double = {
    totalMorphemesScoreAtSet(redisKeyObserved)
  }

  /**
   * Returns the total number of members in the observed
   * set
   */
  def observedZCard: Int = {
    zCard(redisKeyObserved)
  }

  /**
   * Returns the chi-squared score for a given term
   *
   * @param term that we want to calculate chisquared for
   * @param observedScoreForTerm the observed score for the term
   * @param expectedSetTotalScore the total number of times all morphemes
   *  appeared in the expected set
   * @param observedSetTotalScore the total number of times all morphemes
   *  appeared in the observed set
   */
  def chiSquaredForTerm(
    term: String,
    observedScoreForTerm: Double,
    expectedSetTotalScore: Double,
    observedSetTotalScore: Double): Double = {
    val expectedScoreForTerm = getExpectedScoreForTerm(term)
    if (observedScoreForTerm > expectedScoreForTerm)
      calculateChiSquaredForTerm(
        expectedScoreForTerm,
        observedScoreForTerm,
        expectedSetTotalScore,
        observedSetTotalScore)
    else
      0
  }

  /**
   * Returns the chi-squared score for a given term
   *
   * Essentially the same as the overridden version except can be called
   * without the observedScoreForTerm parameter
   *
   * @param term that we want to calculate chisquared for
   * @param expectedSetTotalScore the total number of times all morphemes
   *  appeared in the expected set
   * @param observedSetTotalScore the total number of times all morphemes
   *  appeared in the observed set
   */
  def chiSquaredForTerm(term: String, expectedSetTotalScore: Double, observedSetTotalScore: Double): Double = {
    val observedScoreForTerm = getObservedScoreForTerm(term)
    chiSquaredForTerm(term, observedScoreForTerm, expectedSetTotalScore, observedSetTotalScore)
  }

  private def termsWithScoresList(
    redisKey: String,
    min: Double = Double.NegativeInfinity,
    limit: Option[(Int, Int)] = None,
    sort: SortOrder = DESC) = {
    redisPool.withClient { redis =>
      redis.zrangebyscoreWithScore(redisKey, min, limit = limit, sortAs = sort)
    }
  }

  private def newTermsWithScoresListWithLimit(limitDesired: Option[(Int, Int)] = Some(0, 50)) = {
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