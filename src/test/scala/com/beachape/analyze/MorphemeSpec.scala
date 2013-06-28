package com.beachape.analyze

import scala.runtime.ZippedTraversable2.zippedTraversable2ToTraversable

import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class MorphemeSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  val stringToAnalyse = "隣の客はよく柿食う客だ"
  val knownMorphemeSurfaces = List("隣", "の", "客", "は", "よく", "柿", "食う", "客", "だ")

  describe("Morpheme") {

    describe(".stringToMorphemes({anyString})") {

      it("should return a List[Morpheme]") {
        val morphemes = Morpheme.stringToMorphemes(stringToAnalyse)
        morphemes.isInstanceOf[List[Morpheme]] should be(true)
      }

      describe("each morpheme in the list") {

        val morphemes = Morpheme.stringToMorphemes(stringToAnalyse)

        describe(".surface") {

          it("should be a string") {
            for (m <- morphemes)
              m.surface.isInstanceOf[String] should be(true)
          }

          it("should match one for one with the known list") {
            for ((m: Morpheme, km: String) <- (morphemes, knownMorphemeSurfaces).zipped.toList)
              m.surface should be(km)
          }

        }

      }

    }

    describe(".stringToWords({anyString})") {

      it("should return a List[String]") {
        val words = Morpheme.stringToWords(stringToAnalyse)
        words.isInstanceOf[List[String]] should be(true)
      }

    }

  }

}