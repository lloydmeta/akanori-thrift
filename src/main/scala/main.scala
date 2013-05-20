import org.chasen.mecab.{Tagger, Node, MeCab}

class NodeContainer(node: Node) {

  private var nodes = List(node)
  var working_node = node.getNext
  while (working_node != null){
    nodes = nodes ::: List(working_node)
    working_node =  working_node.getNext
  }

  def list = nodes

}

object TestScalaMecab extends App {

  override def main(args: Array[String]) {
    System.loadLibrary("MeCab")

    var tagger = new Tagger

    val str = "まず、僕のsbt のバージョンはくず"
    println(tagger.parse(""))

    val node = tagger.parseToNode(str)
    val container = new NodeContainer(node)
    for (n <- container.list ) {
      println(n.getSurface)
    }
  }
}