import org.chasen.mecab.{ Tagger, Node, MeCab }
import org.beachape.analyze.{ Morpheme, MorphemesRedisRetriever, FileMorphemesToRedis }
import akka.actor.Actor
import akka.actor.ActorSystem
import akka.actor.Props
import com.redis._
import java.io._
import org.beachape.actors._
import org.beachape.server.TrendServer
import trendServer.gen.TrendThriftServer
import org.apache.thrift._
import org.apache.thrift.protocol._
import org.apache.thrift.server._
import org.apache.thrift.transport._

object TrendApp {


  val usage = """
      Usage: TrendApp
          --file-older-expected path
          --file-older-observed path
          --file-newer-expected path
          --file-newer-observed path
          [--min-occurrence Int, defaults to 10]
          [--min-length Int, defaults to 1]
          [--max-length Int, defaults to 50]
          [--top Int, defaults to 50]
          [--drop-blacklisted Boolean, defaults to true]
          [--only-whitelisted Boolean, defaults to false]
          [--redis-host String, defaults to localhost]
          [--redis-db Int, defaults to 0]
          [--redis-port Int, defaults to 6379]
    """

  def main(args: Array[String]) {

    def printUsageAndExit[T](default: T = "String"): T = {
      println(usage)
      exit(1)
      default
    }

    if (args.length == 0) printUsageAndExit()
    val arglist = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      def isSwitch(s: String) = (s(0) == '-')
      list match {
        case Nil => map
        case "--file-older-expected" :: value :: tail =>
          nextOption(map ++ Map('fileOlderExpected -> value), tail)
        case "--file-older-observed" :: value :: tail =>
          nextOption(map ++ Map('fileOlderObserved -> value), tail)
        case "--file-newer-expected" :: value :: tail =>
          nextOption(map ++ Map('fileNewerExpected -> value), tail)
        case "--file-newer-observed" :: value :: tail =>
          nextOption(map ++ Map('fileNewerObserved -> value), tail)
        case "--min-occurrence" :: value :: tail =>
          nextOption(map ++ Map('minOccurrence -> value.toDouble), tail)
        case "--min-length" :: value :: tail =>
          nextOption(map ++ Map('minLength -> value.toInt), tail)
        case "--max-length" :: value :: tail =>
          nextOption(map ++ Map('maxLength -> value.toInt), tail)
        case "--top" :: value :: tail =>
          nextOption(map ++ Map('top -> value.toInt), tail)
        case "--drop-blacklisted" :: value :: tail =>
          nextOption(map ++ Map('dropBlacklisted -> value.toBoolean), tail)
        case "--only-whitelisted" :: value :: tail =>
          nextOption(map ++ Map('onlyWhitelisted -> value.toBoolean), tail)
        case "--redis-host" :: value :: tail =>
          nextOption(map ++ Map('redisHost -> value), tail)
        case "--redis-port" :: value :: tail =>
          nextOption(map ++ Map('redisPort -> value.toInt), tail)
        case "--redis-db" :: value :: tail =>
          nextOption(map ++ Map('redisDb -> value.toInt), tail)
        case option :: tail =>
          println("Unknown option " + option)
          exit(1)
      }
    }

    val options = nextOption(Map(), arglist)

    val oldExpectedFilePath: String = options.get('fileOlderExpected) match {
      case Some(x: String) => x
      case _ => printUsageAndExit()
    }

    val oldObservedFilePath: String = options.get('fileOlderObserved) match {
      case Some(x: String) => x
      case _ => printUsageAndExit()
    }

    val newExpectedFilePath: String = options.get('fileNewerExpected) match {
      case Some(x: String) => x
      case _ => printUsageAndExit()
    }

    val newObservedFilePath: String = options.get('fileNewerObserved) match {
      case Some(x: String) => x
      case _ => printUsageAndExit()
    }

    val minLength = options.getOrElse('minLength, 1).asInstanceOf[Int]
    val maxLength = options.getOrElse('maxLength, 50).asInstanceOf[Int]
    val top = options.getOrElse('top, 50).asInstanceOf[Int]
    val onlyWhitelisted = options.getOrElse('onlyWhitelisted, false).asInstanceOf[Boolean]
    val dropBlacklisted = options.getOrElse('dropBlacklisted, true).asInstanceOf[Boolean]
    val minOccurrence = options.getOrElse('minOccurrence, 10.0).asInstanceOf[Double]
    val redisHost = options.getOrElse('redisHost, "localhost").toString
    val redisPort = options.getOrElse('redisPort, 6379).asInstanceOf[Int]
    val redisDb = options.getOrElse('redisPort, 7).asInstanceOf[Int]

    val redisPool = new RedisClientPool(redisHost, redisPort, database= redisDb)
    redisPool.withClient {_.flushdb}

    val system = ActorSystem("akanoriSystem")

    val mainOrchestrator = system.actorOf(MainOrchestrator(redisPool, dropBlacklisted, onlyWhitelisted, minOccurrence, minLength, maxLength, top), "mainOrchestrator")

    mainOrchestrator ! FullFilePathSet(FilePathSet(FilePath(oldExpectedFilePath), FilePath(oldObservedFilePath)), FilePathSet(FilePath(newExpectedFilePath), FilePath(newObservedFilePath)))

    val st = new TServerSocket(9090)
    val processor = new TrendThriftServer.Processor(new TrendServer(mainOrchestrator))
    val arg = new TThreadPoolServer.Args(st)
    arg.processor(processor)

    val server = new TThreadPoolServer(arg)

    server.serve()

  }
}