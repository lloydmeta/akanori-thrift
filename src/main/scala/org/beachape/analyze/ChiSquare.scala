package org.beachape.analyze
import scala.math.pow

trait ChiSquare {

  def calculateChiSquaredForTerm(oldTermScore: Double, termScore: Double, oldSetTotalScore: Double, newSetTotalScore: Double): Double = {

    // Calculate frequencies
    val observedTermFrequency = termScore / newSetTotalScore
    val expectedTermFrequency = oldTermScore / oldSetTotalScore
    val otherObservedFrequency = (newSetTotalScore - termScore) / newSetTotalScore
    val otherExpectedFrequency = (oldSetTotalScore - oldTermScore) / oldSetTotalScore

    val termChiSquaredPart = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency)
    val otherChiSquaredPart = calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency)

    termChiSquaredPart + otherChiSquaredPart
  }

  private def calculateChiSquaredPart(expectedFrequency: Double, observedFrequency: Double) = {
    pow(((observedFrequency * 100 - expectedFrequency * 100).abs - 0.5), 2) / (100 * expectedFrequency)
  }
}