package org.beachape.analyze
import scala.math.pow

trait ChiSquare {

  def calculateChiSquaredForTerm(oldTermScore: Double, termScore: Double, oldSetTotalScore: Double, newSetTotalScore: Double): Double = {

    // Calculate frequencies with add 1 smoothing
    val observedTermFrequency = termScore / newSetTotalScore + 1
    val expectedTermFrequency = oldTermScore / oldSetTotalScore + 1
    val otherObservedFrequency = (newSetTotalScore - termScore) / newSetTotalScore + 1
    val otherExpectedFrequency = (oldSetTotalScore - oldTermScore) / oldSetTotalScore + 1

    val termChiSquaredPart = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency)
    val otherChiSquaredPart = calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency)

    termChiSquaredPart + otherChiSquaredPart
  }

  private def calculateChiSquaredPart(expectedFrequency: Double, observedFrequency: Double) = {
    pow(((observedFrequency * 100 - expectedFrequency * 100).abs - 0.5), 2) / (100 * expectedFrequency)
  }
}