import org.beachape.support.RichRange._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers

class RichRangeSpec extends FunSpec
                       with ShouldMatchers {

  describe("#listOfConsecutivePairsInSteps") {

    val range = (1 to 30)

    it("should return a list with doubles separated by specified step") {
      val list = range.listOfConsecutivePairsInSteps(5)
      list take (list.length - 1) foreach (x =>
        (x._2 - x._1) should be (5)
      )
    }

    it("should return a list with the last double in the list having the end of the range as the second element in the pair") {
      val list = range.listOfConsecutivePairsInSteps(5)
      list drop (list.length - 1) foreach (x =>
        x._2 should be (range.end)
      )
    }

  }

}