package org.beachape.analyze
import scala.math.pow

trait ChiSquare {

  def calculateChiSquaredForTerm(oldTermScore: Double, termScore: Double, oldSetTotalScore: Double, newSetTotalScore: Double): Double = {

    // Calculate frequencies with add 1 smoothing
    val observedTermFrequency = termScore / (if (newSetTotalScore > 0) newSetTotalScore else 1)
    val expectedTermFrequency = adjustedExpectedFrequency(oldTermScore, oldSetTotalScore)
    val otherObservedFrequency = (newSetTotalScore - termScore) / (if (newSetTotalScore > 0) newSetTotalScore else 1)
    val otherExpectedFrequency = adjustedExpectedFrequency((oldSetTotalScore - oldTermScore), oldSetTotalScore)

    val termChiSquaredPart = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency)
    val otherChiSquaredPart = calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency)

    termChiSquaredPart + otherChiSquaredPart
  }

  private def calculateChiSquaredPart(expectedFrequency: Double, observedFrequency: Double) = {
    pow(((observedFrequency * 100 - expectedFrequency * 100).abs - 0.5), 2) / (expectedFrequency * 100)
  }

  private def adjustedExpectedFrequency(termScore: Double, totalScore: Double) = {
    val frequency = termScore / (if (totalScore > 0) totalScore else 1)
    if (frequency == 0) (1.0 / totalScore) else frequency
  }
}