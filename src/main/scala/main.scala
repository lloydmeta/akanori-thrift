import org.chasen.mecab.{Tagger, Node, MeCab}
import collection.immutable._

class NodeContainer(node: Node) {

  private var nodes = List(node)
  var working_node = node.getNext()
  while (working_node != null){
    println(working_node.getSurface)
    nodes = working_node :: nodes
    working_node =  working_node.getNext()
  }

  def list: List[Node] = nodes

}

object TestScalaMecab extends App {

  override def main(args: Array[String]) {
    System.loadLibrary("MeCab")

    val tagger = new Tagger

    val str = "まず、僕のsbt のバージョンはくず";

    var node = tagger.parseToNode(str);

    while(node != null){
      println(node.getSurface() + "\t" + node.getFeature());
      node = node.getNext();
    }


    // val node = tagger.parseToNode(str);
    // val container = new NodeContainer(node)
    // for (n <- container.list ) {
    //   println(n.getSurface)
    // }
  }
}