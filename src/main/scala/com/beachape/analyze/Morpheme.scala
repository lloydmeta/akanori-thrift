// from http://blog.kenkov.jp/2013/04/04/mecab_scala.html
package com.beachape.analyze

import scala.collection.JavaConversions._
import org.atilika.kuromoji.Tokenizer

/**
 *  Companion / Factory object for instantiating a List of morphemes
 *  (individual components, e.g. nouns, words, etc) from a single
 *  Japanese string.
 *
 *  Depends on the Mecab-Java library for parsing. This allows us to
 *  take a Japanese string (with no spaces) and break it up into smaller
 *  components that can then be used for trend detection.
 */
object Morpheme {

  type Words = List[String]

  val attributeValueBlackistMap = Map(
    'surface -> (
      List("ﾟ", "д", "Д", "ーーー", "ーー", "ー", "ｰ", "m", "目", "о", "ぐはじめました")
      ::: ('A' until 'z').toList
      ::: ('ぁ' until 'ゟ').toList
      ::: ('゠' until 'ヿ').toList
      ::: ('ｦ' until '￮').toList).map(_.toString),
    'hinsi -> List("代名詞", "助詞", "記号", "接続詞", "形容詞", "助動詞"),
    'hinsi1 -> List("助数詞", "接尾", "数", "サ変接続", "非自立"))

  val attributeValueWhitelistMap = Map(
    'hinsi -> List("名詞"))

  val blackListFilter = { (x: Morpheme) =>
    !attributeValueBlackistMap.getOrElse('surface, List()).contains(x.surface) &&
      !attributeValueBlackistMap.getOrElse('hinsi, List()).contains(x.hinsi) &&
      !attributeValueBlackistMap.getOrElse('hinsi1, List()).contains(x.hinsi1)
  }

  val whiteListFilter = { (x: Morpheme) =>
    attributeValueWhitelistMap.getOrElse('hinsi, List()).contains(x.hinsi)
  }

  /**
   * Returns a list of Morpheme objects based on a string passed in
   * and onlyBlacklisted and onlyWhitelisted options
   *
   * @param str the string to parse into a list of Morphemes
   * @param dropBlacklisted drop blacklisted morphemes
   * @param onlyWhitelisted return only whitelisted morphemes
   */
  def stringToMorphemes(str: String, dropBlacklisted: Boolean = false, onlyWhitelisted: Boolean = false): List[Morpheme] = {
    stringToMorphemesReverse(str, dropBlacklisted, onlyWhitelisted).reverse
  }

  /**
   *  Returns a list of morphemes in reverse order of the way
   *  they appear in the string passed in.
   *
   *  Slightly faster variant of stringToMorphemes because the list
   *  is constructed via :: and then returned without running .reverse
   */
  def stringToMorphemesReverse(str: String, dropBlacklisted: Boolean = false, onlyWhitelisted: Boolean = false): List[Morpheme] = {
    val tokenizer = Tokenizer.builder().build()
    val tokens = tokenizer.tokenize(str).toList

    val morphemes = for (token <- tokens) yield parseMorpheme(token.getSurfaceForm, token.getAllFeatures)

    if (dropBlacklisted && onlyWhitelisted)
      morphemes.filter(whiteListFilter).filter(blackListFilter)
    else if (dropBlacklisted)
      morphemes.filter(blackListFilter)
    else if (onlyWhitelisted)
      morphemes.filter(whiteListFilter)
    else
      morphemes
  }

  /**
   * Returns a List[String], aka Words based on a
   * string passed in
   *
   * @param str String to parse into individual words
   */
  def stringToWords(str: String): Words = {
    stringToMorphemes(str) map { _.surface }
  }

  private def parseMorpheme(surface: String, features: String): Morpheme = {
    val data = features.split(",").toList
    val data_adjusted = data.length match {
      case 9 => data
      case x if x < 9 => data ::: (for (i <- 1 to (9 - x)) yield "*").toList
    }
    println(data_adjusted)
    new Morpheme(surface,
      data_adjusted(0),
      data_adjusted(1),
      data_adjusted(2),
      data_adjusted(3),
      data_adjusted(4),
      data_adjusted(5),
      data_adjusted(6),
      data_adjusted(7),
      data_adjusted(8))
  }
}

/**
 * Holds a morpheme's surface value (what it appeared as in
 * the string used to create it), and other information
 *
 * Should be instantiated via the factory methods in the companion
 * object above
 */
class Morpheme(val surface: String,
  val hinsi: String,
  val hinsi1: String,
  val hinsi2: String,
  val hinsi3: String,
  val katuyoukei: String,
  val katuyougata: String,
  val genkei: String,
  val yomi: String,
  val hatuon: String) {

  override def toString: String = {
    "Morpheme(" + List(surface, hinsi, hinsi1, hinsi2, hinsi3,
      katuyoukei, katuyougata, genkei, yomi, hatuon).mkString(",") + ")"
  }

  override def equals(that: Any): Boolean = that match {
    case other: Morpheme =>
      other.surface == surface &&
        other.hinsi == hinsi &&
        other.hinsi1 == hinsi1 &&
        other.hinsi2 == hinsi2 &&
        other.hinsi3 == hinsi3 &&
        other.katuyoukei == katuyoukei &&
        other.katuyougata == katuyougata &&
        other.genkei == genkei &&
        other.yomi == yomi &&
        other.hatuon == hatuon
    case _ => false
  }
}
