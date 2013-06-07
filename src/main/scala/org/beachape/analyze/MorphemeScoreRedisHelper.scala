package org.beachape.analyze

trait MorphemeScoreRedisHelper {
  def zSetTotalScoreKey = "{__akanori_score_counter__}"
}