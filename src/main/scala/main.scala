import org.chasen.mecab.{Tagger, Node, MeCab}
import org.beachape.analyze.Morpheme
import org.beachape.support.FileWrite.printToFile
import java.io._

object TestScalaMecab extends App {

  override def main(args: Array[String]) {
    val str = "まず、僕のsbt のバージョンはくず"
    val f = new File("/Users/a13075/Desktop/test")

    val warmup = Morpheme.stringToWords(str) //Dont know why this is necessary
    val words = Morpheme.stringToWords(str)

    printToFile(f)(p => {
      words.foreach(p.println)
    })
  }
}