package org.beachape.analyze
import scala.math.pow

trait ChiSquare {

  def calculateChiSquaredForTerm(oldTermScore: Double, termScore: Double, oldSetTotalScore: Double, newSetTotalScore: Double): Double = {

    // Calculate frequencies with add 1 smoothing
    val expectedTotalScoreAdjusted = if (oldSetTotalScore > 0) oldSetTotalScore else 1
    val expectedTermFrequency = oldTermScore / expectedTotalScoreAdjusted
    val expectedTermFrequencyMin = 1.0 / expectedTotalScoreAdjusted

    val observedTermFrequency = termScore / newSetTotalScore

    val otherExpectedFrequency = (oldSetTotalScore - oldTermScore) / expectedTotalScoreAdjusted
    val otherExpectedFrequencyMin = 1.0 / expectedTotalScoreAdjusted

    val otherObservedFrequency = (newSetTotalScore - termScore) / newSetTotalScore

    val termChiSquaredPart = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency, expectedTermFrequencyMin)
    val otherChiSquaredPart = calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency, otherExpectedFrequencyMin)

    termChiSquaredPart + otherChiSquaredPart
  }

  private def calculateChiSquaredPart(expectedFrequency: Double, observedFrequency: Double, expectedFrequencyMin: Double) = {
    pow(((observedFrequency * 100 - expectedFrequency * 100).abs - 0.5), 2) /
      ((if (expectedFrequency == 0) expectedFrequencyMin else expectedFrequency) * 100)
  }

}