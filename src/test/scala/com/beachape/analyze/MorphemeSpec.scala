package com.beachape.analyze

import scala.runtime.ZippedTraversable2.zippedTraversable2ToTraversable

import org.scalatest.{PrivateMethodTester, BeforeAndAfterAll, BeforeAndAfterEach, FunSpec}
import org.scalatest.matchers.ShouldMatchers
import org.atilika.kuromoji.Tokenizer
import java.io.{FileWriter, BufferedWriter}
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import akka.util.Timeout
import akka.agent.Agent

class MorphemeSpec extends FunSpec
  with ShouldMatchers
  with PrivateMethodTester
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

    describe("tokenizer accessors") {

      describe(".tokenizer") {

        it("should return a tokenizer") {
          Morpheme.tokenizer.isInstanceOf[Tokenizer] should be(true)
        }
      }

      describe(".tokenizer_=") {

        implicit val timeout = Timeout(5 seconds)
        val privateTokenizerAgentAccessor = PrivateMethod[Agent[Tokenizer]]('tokenizerAgent)

        it("should assign a new tokenizer") {
          // Create a temporary dictionary file
          val tempFile = java.io.File.createTempFile("fakeDictionary", ".txt")
          val writer = new BufferedWriter(new FileWriter(tempFile))
          writer.write(
            """
              |##
              |## This file should use UTF-8 encoding
              |##
              |## User dictionary format:
              |##   <text>,<token1> <token2> ... <tokenn>,<reading1> <reading2> ... <readingn>,<part-of-speech>
              |##
              |
              |# Custom segmentation for long entries
              |日本経済新聞,日本 経済 新聞,ニホン ケイザイ シンブン,カスタム名詞
              |関西国際空港,関西 国際 空港,カンサイ コクサイ クウコウ,テスト名詞
              |
              |# Custom reading for former sumo wrestler Asashoryu
              |朝青龍,朝青龍,アサショウリュウ,カスタム人名
            """.stripMargin)
          writer.close

          //Note that we wait to make sure that the Agent has had all updates written to
          val newTokenizer = Tokenizer.builder().userDictionary(tempFile.getAbsolutePath).build()
          val newTokenizerTokens = newTokenizer.tokenize("朝青龍")
          val defaultTokenizer = Morpheme.invokePrivate(privateTokenizerAgentAccessor()).await
          val defaultTokenizerTokens = defaultTokenizer.tokenize("朝青龍")
          defaultTokenizerTokens.head.getSurfaceForm should not be(newTokenizerTokens.head.getSurfaceForm)

          Morpheme.tokenizer = newTokenizer
          val assignedTokenizer = Morpheme.invokePrivate(privateTokenizerAgentAccessor()).await
          val assignedTokenizerTokens = assignedTokenizer.tokenize("朝青龍")
          assignedTokenizerTokens.head.getSurfaceForm should be(newTokenizerTokens.head.getSurfaceForm)
        }
      }
    }

  }

}