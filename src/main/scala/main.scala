import org.chasen.mecab.{ Tagger, Node, MeCab }
import org.beachape.analyze.{ Morpheme, MorphemesRedisRetriever, FileMorphemesToRedis }
import com.redis._
import java.io._

object TrendApp {

  val usage = """
      Usage: TrendApp
		  --file-older-expected path
		  --file-older-observed path
		  --file-newer-expected path
		  --file-newer-observed path
          [--report-newer-chisquared Boolean, defaults to false]
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
        case "--report-newer-chisquared" :: value :: tail =>
          nextOption(map ++ Map('reportNewerChiSquared -> value.toBoolean), tail)
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
    val reportNewerChiSquared = options.getOrElse('reportNewerChiSquared, true).asInstanceOf[Boolean]
    val redisHost = options.getOrElse('redisHost, "localhost").toString
    val redisPort = options.getOrElse('redisPort, 6379).asInstanceOf[Int]
    val redisDb = options.getOrElse('redisPort, 7).asInstanceOf[Int]

    val redis = new RedisClient(redisHost, redisPort)
    redis.select(redisDb)
    redis.flushdb

    val oldExpectedSetRedisKey = "trends:old:expected"
    val oldObservedSetRedisKey = "trends:old:observed"
    val newExpectedSetRedisKey = "trends:new:expected"
    val newObservedSetRedisKey = "trends:new:observed"

    val oldExpectedFileOrchestrator = FileMorphemesToRedis(oldExpectedFilePath, redis, oldExpectedSetRedisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
    val oldObservedFileOrchestrator = FileMorphemesToRedis(oldObservedFilePath, redis, oldObservedSetRedisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
    val newExpectedFileOrchestrator = FileMorphemesToRedis(newExpectedFilePath, redis, newExpectedSetRedisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
    val newObservedFileOrchestrator = FileMorphemesToRedis(newObservedFilePath, redis, newObservedSetRedisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)

    println("Dumping old expected set to Redis...")
    oldExpectedFileOrchestrator.dumpToRedis
    println("Dumping old observed set to Redis...")
    oldObservedFileOrchestrator.dumpToRedis
    println("Dumping new expected set to Redis...")
    newExpectedFileOrchestrator.dumpToRedis
    println("Dumping new observed set to Redis...")
    newObservedFileOrchestrator.dumpToRedis

    val oldChiSquaredRetriever = MorphemesRedisRetriever(redis, oldExpectedSetRedisKey, oldObservedSetRedisKey)
    val newChiSquaredRetriever = MorphemesRedisRetriever(redis, newExpectedSetRedisKey, newObservedSetRedisKey)

    println("Generating ChiSquare for Old expected vs observed and dumping to Redis")
    val oldChiSquaredKey = oldChiSquaredRetriever.storeChiSquared

    println("Generating ChiSquare for New expected vs observed and dumping to Redis")
    val newChiSquaredKey = newChiSquaredRetriever.storeChiSquared

    val chiSquaredRetreiver = MorphemesRedisRetriever(redis, oldChiSquaredKey, newChiSquaredKey)

    if (reportNewerChiSquared) {
      println("\nChiSquared for New Set")
      println("**********************")
      for ((term, chiScore) <- newChiSquaredRetriever.byChiSquaredReversed.filter(x => x._1.length >= minLength && x._1.length <= maxLength).take(top))
        println(s"Term: [$term], χ² score $chiScore")
      println("**********************\n")
    }

    println("ChiSquared")
    println("**********")
    for ((term, chiScore) <- chiSquaredRetreiver.byChiSquaredReversed.filter(x => x._1.length >= minLength && x._1.length <= maxLength).take(top))
      println(s"Term: [$term], χ² score $chiScore")

  }
}