import org.beachape.support.RichRange.range2RichRange
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class RichRangeSpec extends FunSpec
  with ShouldMatchers {

  describe("#listOfConsecutivePairsInSteps") {

    val range = (1 to 30)

    it("should return a list with doubles separated by specified step") {
      val list = range.listOfConsecutivePairsInSteps(5)
      list take (list.length - 1) foreach (x =>
        (x._2 - x._1) should be(5))
    }

    it("should return a list with the last double in the list having the end of the range as the second element in the pair") {
      val list = range.listOfConsecutivePairsInSteps(5)
      list drop (list.length - 1) foreach (x =>
        x._2 should be(range.end))
    }

    it("should return me a list with a single double with start and end as ._1 and ._2 if the range is smaller than the step") {
      val list = range.listOfConsecutivePairsInSteps(50)
      list.head should be((1, 30))
    }

    it("should return a list with the proper number of items") {
      val list = range.listOfConsecutivePairsInSteps(5)
      list.length should be(5)
    }

    it("should return a list where each consecutive element's._1 is equal to the the previous element's ._2") {
      val list = range.listOfConsecutivePairsInSteps(5)
      list match {
        case x :: y :: xs => x._2 should be(y._1)
        case _ =>
      }
    }

  }

}