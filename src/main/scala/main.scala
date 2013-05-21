import org.chasen.mecab.{Tagger, Node, MeCab}
import org.beachape.analyze.Morpheme


object TestScalaMecab extends App {

  override def main(args: Array[String]) {
    System.loadLibrary("MeCab")

    val str = "まず、僕のsbt のバージョンはくず"

    println(Morpheme.stringToWords(str))
  }
}