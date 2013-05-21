import org.beachape.analyze.Morpheme
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

class MorphemeSpec extends FunSpec
                       with ShouldMatchers
                       with BeforeAndAfterEach
                       with BeforeAndAfterAll {

  val stringToAnalyse = "隣の客はよく柿食う客だ"

  describe("Morpheme") {

    describe(".stringToMorphemes({anyString})") {

      it ("should return a List[Morpheme]") {
        val morphemes = Morpheme.stringToMorphemes(stringToAnalyse)
        for (m <- morphemes)
          m.isInstanceOf[Morpheme] should be (true)
      }

    }

  }

}