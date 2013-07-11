import com.beachape.analyze.Morpheme
import com.typesafe.scalalogging.slf4j.Logging
import scala.concurrent.duration.DurationInt

import com.beachape.actors.MainActorSystem._ //implicit ActorSystem
import com.beachape.actors.{DictionaryMonitorActor, GenerateDefaultTrends, MainOrchestrator}
import com.beachape.server.TrendServer
import com.beachape.support.SampleFileToRedisDumper
import com.redis.RedisClientPool

import akka.routing.SmallestMailboxRouter

import scala.language.postfixOps

import org.atilika.kuromoji.Tokenizer

object TrendApp extends Logging {

  val usage = """
      Usage: Akanori-thrift (options are for currentTrendsDefault)
          --clear-redis Boolean
          [--thrift-server-port Int, defaults to 9090]
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
          [--custom-dictionary-path String, path to a dictionary file (txt). See src/example/customDictionary.txt]
      """

  def main(args: Array[String]) {

    def printUsageAndExit[T](default: T = "String"): T = {
      logger.error(usage)
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
        case "--thrift-server-port" :: value :: tail =>
          nextOption(map ++ Map('thriftServerPort -> value.toInt), tail)
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
        case "--sample-data-filepath" :: value :: tail =>
          nextOption(map ++ Map('sampleDataFilepath -> value), tail)
        case "--sample-data-from" :: value :: tail =>
          nextOption(map ++ Map('sampleDataFrom -> value.toInt), tail)
        case "--sample-data-until" :: value :: tail =>
          nextOption(map ++ Map('sampleDataUntil -> value.toInt), tail)
        case "--custom-dictionary-path" :: value :: tail =>
          nextOption(map ++ Map('customDictionaryPath -> value), tail)
        case option :: tail =>
          logger.error("Unknown option " + option)
          sys.exit(1)
      }
    }

    val options = nextOption(Map(), arglist)

    val clearRedis: Boolean = options.get('clearRedis) match {
      case Some(x) => x.asInstanceOf[Boolean]
      case _ => printUsageAndExit(true)
    }
    val thriftServerPort = options.getOrElse('thriftServerPort, 9090).asInstanceOf[Int]
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
    val sampleDataFilepath = options.getOrElse('sampleDataFilepath, "").toString
    val sampleDataFrom = options.getOrElse('sampleDataFrom,
      (System.currentTimeMillis / 1000 - 259200).toInt).asInstanceOf[Int]
    val sampleDataUntil = options.getOrElse('sampleDataUntil,
      (System.currentTimeMillis / 1000).toInt).asInstanceOf[Int]
    val customDictionaryPath = options.getOrElse('customDictionaryPath, "").toString

    val redisPool = new RedisClientPool(redisHost, redisPort, database = redisDb)
    if (clearRedis) redisPool.withClient { _.flushdb }

    val mainOrchestratorRoundRobin = system.actorOf(
      MainOrchestrator(
        redisPool,
        dropBlacklisted,
        onlyWhitelisted,
        spanInSeconds,
        minOccurrence,
        minLength,
        maxLength,
        top).
        withRouter(SmallestMailboxRouter(3)), "mainOrchestrator")

    import system.dispatcher
    val generateDefaultTrendsCancellableSchedule = system.scheduler.schedule(
      5 seconds,
      1 minute,
      mainOrchestratorRoundRobin, GenerateDefaultTrends)

    if (!sampleDataFilepath.isEmpty) {
      logger.info(s"Dumping sample data from file")
      SampleFileToRedisDumper(redisPool).dumpToRedis(sampleDataFilepath, sampleDataFrom, sampleDataUntil)
    }

    if (!customDictionaryPath.isEmpty) {
      logger.info(s"Attempting to use dictionary from: $customDictionaryPath")
      try {
        val customDictionaryTokenizer = Tokenizer.builder().userDictionary(customDictionaryPath).build()
        Morpheme.tokenizer = customDictionaryTokenizer
      } catch {
        case e:java.lang.ArrayIndexOutOfBoundsException => logger.error("Formatting seems borked on the custom dictionary file, try fixing it (restart not required)")
        case e:Exception => logger.error("Something went wrong when trying to use the custom dictionary file, try fixing it (restart not required)")
      }
      system.actorOf(DictionaryMonitorActor(customDictionaryPath), "customDictionaryMonitor")
    }

    logger.debug("Server is ready for duty.")
    val server = TrendServer(mainOrchestratorRoundRobin, thriftServerPort)
    server.serve
  }
}