// from http://blog.kenkov.jp/2013/04/04/mecab_scala.html
package org.beachape.analyze

import org.chasen.mecab.{ MeCab, Tagger, Node }

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

  def stringToMorphemes(str: String, dropBlacklisted: Boolean = false, onlyWhitelisted: Boolean = false): List[Morpheme] = {
    System.loadLibrary("MeCab")
    val tagger = new Tagger
    val node = {
      tagger.parseToNode(str) //wtf
      tagger.parseToNode(str)
    }
    val morphemes = nodeListFromNode(node) map (x => parseMorpheme(x.getSurface.trim, x.getFeature))

    val justMorphemes = morphemes dropRight 1 drop 1

    if (dropBlacklisted && onlyWhitelisted)
      justMorphemes.filter(whiteListFilter).filter(blackListFilter)
    else if (dropBlacklisted)
      justMorphemes.filter(blackListFilter)
    else if (onlyWhitelisted)
      justMorphemes.filter(whiteListFilter)
    else
      justMorphemes
  }

  def stringToWords(str: String): Words = {
    stringToMorphemes(str) map { _.surface }
  }

  private def nodeListFromNode(node: Node): List[Node] = {
    var nodes = List(node)
    var working_node = node.getNext
    while (working_node != null) {
      nodes = nodes ::: List(working_node)
      working_node = working_node.getNext
    }
    nodes
  }

  private def parseMorpheme(surface: String, chasenData: String): Morpheme = {
    val data = chasenData.split(",").toList
    val data_adjusted = data.length match {
      case 9 => data
      case x if x < 9 => data ::: (for (i <- 1 to (9 - x)) yield "*").toList
    }
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
