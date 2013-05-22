package org.beachape.analyze
import scala.math.pow

trait ChiSquare {

  def calculateChiSquaredForTerm(oldTermScore: Double, termScore: Double, oldSetTotalScore: Double, newSetTotalScore: Double): Double = {

    // Calculate frequencies
    val observedTermFrequency = termScore / newSetTotalScore
    val expectedTermFrequency = oldTermScore / oldSetTotalScore
    val otherObservedFrequency = (newSetTotalScore - termScore) / newSetTotalScore
    val otherExpectedFrequency = (oldSetTotalScore - oldTermScore) / oldSetTotalScore

    val normalizer = List(newSetTotalScore, oldSetTotalScore).max

    val termChiSquaredPart = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency, normalizer)
    val otherChiSquaredPart = calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency, normalizer)

    termChiSquaredPart + otherChiSquaredPart
  }

  private def calculateChiSquaredPart(expectedFrequency: Double, observedFrequency: Double, normalizer: Double) = {
    pow(((observedFrequency * normalizer - expectedFrequency * normalizer).abs - 0.5), 2) / (normalizer * expectedFrequency)
  }
}