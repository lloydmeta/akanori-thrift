package org.beachape.actors

/** Provides a single helper to standardize
 *  the member used for tracking the total score of
 *  a sorted set
 */
trait MorphemeScoreRedisHelper {
  def zSetTotalScoreKey = "{__akanori_score_counter__}"
}