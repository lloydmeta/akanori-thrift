import org.beachape.analyze.ChiSquare
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import scala.math.pow

class DummyClass extends ChiSquare

class ChisquareSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  val dummy = new DummyClass

  val calculateChiSquaredPart = { (expectedFrequency: Double, observedFrequency: Double) =>
    pow(((observedFrequency * 100 - expectedFrequency * 100).abs - 0.5), 2) / (100 * expectedFrequency)
  }

  describe("#calculateChiSquaredForTerm") {

    it("should return the proper value") {
      val expectedInterestedOccurence = 10.0
      val expectedOtherOccurence = 20.0
      val expectedTotal = expectedInterestedOccurence + expectedOtherOccurence

      val observedInterestedOccurence = 50.0
      val observedOtherOccurence = 80.0
      val observedTotal = observedInterestedOccurence + observedOtherOccurence

      val observedTermFrequency = observedInterestedOccurence / observedTotal
      val expectedTermFrequency = expectedInterestedOccurence / expectedTotal

      val otherObservedFrequency = observedOtherOccurence / observedTotal
      val otherExpectedFrequency = expectedOtherOccurence / expectedTotal

      val assertTotal = calculateChiSquaredPart(expectedTermFrequency, observedTermFrequency) + calculateChiSquaredPart(otherExpectedFrequency, otherObservedFrequency)
      dummy.calculateChiSquaredForTerm(expectedInterestedOccurence, observedInterestedOccurence, expectedTotal, observedTotal) should be(assertTotal)
    }

  }

}