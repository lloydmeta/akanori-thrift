import org.chasen.mecab.{ Tagger, Node, MeCab }
import org.beachape.analyze.{ Morpheme, MorphemesRedisRetriever }
import akka.actor.{ Actor, ActorSystem, Props }
import akka.routing.SmallestMailboxRouter
import com.redis._
import java.io._
import org.beachape.actors._
import org.beachape.server.TrendServer
import trendServer.gen.TrendThriftServer
import org.apache.thrift._
import org.apache.thrift.protocol._
import org.apache.thrift.server._
import org.apache.thrift.transport._
import scala.concurrent.duration._
import scala.language.postfixOps

object TrendApp {

  val usage = """
      Usage: Akanori-thrift (options are for currentTrendsDefault)
          --clear-redis Boolean
          [--span-in-seconds Int, defaults to 3 hours (10800)]
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
      sys.exit(1)
      default
    }

    if (args.length == 0) printUsageAndExit()
    val arglist = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map: OptionMap, list: List[String]): OptionMap = {
      def isSwitch(s: String) = (s(0) == '-')
      list match {
        case Nil => map
        case "--clear-redis" :: value :: tail =>
          nextOption(map ++ Map('clearRedis -> value.toBoolean), tail)
        case "--min-occurrence" :: value :: tail =>
          nextOption(map ++ Map('minOccurrence -> value.toDouble), tail)
        case "--span-in-seconds" :: value :: tail =>
          nextOption(map ++ Map('spanInSeconds -> value.toInt), tail)
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
          sys.exit(1)
      }
    }

    val options = nextOption(Map(), arglist)

    val clearRedis: Boolean = options.get('clearRedis) match {
      case Some(x) => x.asInstanceOf[Boolean]
      case _ => printUsageAndExit(true)
    }
    val spanInSeconds = options.getOrElse('spanInSeconds, 10800).asInstanceOf[Int]
    val minLength = options.getOrElse('minLength, 1).asInstanceOf[Int]
    val maxLength = options.getOrElse('maxLength, 50).asInstanceOf[Int]
    val top = options.getOrElse('top, 50).asInstanceOf[Int]
    val onlyWhitelisted = options.getOrElse('onlyWhitelisted, false).asInstanceOf[Boolean]
    val dropBlacklisted = options.getOrElse('dropBlacklisted, true).asInstanceOf[Boolean]
    val minOccurrence = options.getOrElse('minOccurrence, 10.0).asInstanceOf[Double]
    val redisHost = options.getOrElse('redisHost, "localhost").toString
    val redisPort = options.getOrElse('redisPort, 6379).asInstanceOf[Int]
    val redisDb = options.getOrElse('redisPort, 7).asInstanceOf[Int]

    val redisPool = new RedisClientPool(redisHost, redisPort, database = redisDb)
    if (clearRedis) redisPool.withClient { _.flushdb }

    val system = ActorSystem("akanoriSystem")
    val mainOrchestratorRoundRobin = system.actorOf(MainOrchestrator(redisPool, dropBlacklisted, onlyWhitelisted, spanInSeconds, minOccurrence, minLength, maxLength, top).withRouter(SmallestMailboxRouter(3)), "mainOrchestrator")

    import system.dispatcher
    val generateDefaultTrendsCancellableSchedule = system.scheduler.schedule(5 seconds, 1 minute, mainOrchestratorRoundRobin, List('generateDefaultTrends))

    val st = new TServerSocket(9090)
    val processor = new TrendThriftServer.Processor(new TrendServer(mainOrchestratorRoundRobin))
    val arg = new TThreadPoolServer.Args(st)
    arg.processor(processor)
    val server = new TThreadPoolServer(arg)
    server.serve()

  }
}