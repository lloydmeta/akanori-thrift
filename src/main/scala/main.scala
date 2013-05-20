import org.chasen.mecab.{Tagger, Node, MeCab}
object TestScalaMecab extends App {
  override
  def main(args: Array[String]) {
    System.loadLibrary("MeCab");
    var tagger = new Tagger()
    var str = "まず、僕のsbt のバージョンはくず";
    println(tagger.parse(str));
    var node = tagger.parseToNode(str);

    while(node != null){
      println(node.getSurface() + "\t" + node.getFeature());
      node = node.getNext();
    }
  }
}