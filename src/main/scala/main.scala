import org.chasen.mecab.{Tagger, Node, MeCab}
import org.beachape.analyze.{Morpheme, MorphemesRedisRetriever, FileMorphemesToRedis}
import com.redis._
import java.io._

object TrendApp {

  val usage = """
      Usage: TrendApp --file-older path --file-newer path [--min-length Int, defaults to 1] [--max-length Int, defaults to 50] [--top Int, defaults to 50] [--drop-blacklisted boolean, defaults to true] [--only-whitelisted boolean, defaults to false] [--redis-host address, defaults to localhost] [--redis-db integer, defaults to 0] [--redis-port integer, defaults to 6379]
    """

  def main(args: Array[String]) {

    if (args.length == 0) println(usage)
        val arglist = args.toList
    type OptionMap = Map[Symbol, Any]

    def nextOption(map : OptionMap, list: List[String]) : OptionMap = {
      def isSwitch(s : String) = (s(0) == '-')
      list match {
        case Nil => map
        case "--file-older" :: value :: tail =>
                               nextOption(map ++ Map('fileOlder -> value), tail)
        case "--file-newer" :: value :: tail =>
                               nextOption(map ++ Map('fileNewer -> value), tail)
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
        case option :: tail => println("Unknown option "+option)
                               exit(1)
      }
    }

    val options = nextOption(Map(),arglist)

    val oldFilePath: String = {
      options.get('fileOlder) match {
        case Some(x:String) => x
        case _ =>
          println(usage)
          exit(1)
      }
    }

    val newFilePath: String = {
      options.get('fileNewer) match {
        case Some(x:String) => x
        case _ =>
          println(usage)
          exit(1)
      }
    }

    val minLength = options.getOrElse('minLength, 1).asInstanceOf[Int]
    val maxLength = options.getOrElse('maxLength, 50).asInstanceOf[Int]
    val top = options.getOrElse('top, 50).asInstanceOf[Int]
    val onlyWhitelisted = options.getOrElse('onlyWhitelisted, false).asInstanceOf[Boolean]
    val dropBlacklisted = options.getOrElse('dropBlacklisted, true).asInstanceOf[Boolean]
    val redisHost = options.getOrElse('redisHost, "localhost").toString
    val redisPort = options.getOrElse('redisPort, 6379).asInstanceOf[Int]
    val redisDb = options.getOrElse('redisPort, 7).asInstanceOf[Int]

    val redis = new RedisClient(redisHost, redisPort)
    redis.select(redisDb)
    redis.flushdb

    val oldSetRedisKey = "trends:older"
    val newSetRedisKey = "trends:newer"

    val oldFileOrchestrator = FileMorphemesToRedis(oldFilePath, redis, oldSetRedisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
    val newFileOrchestrator = FileMorphemesToRedis(newFilePath, redis, newSetRedisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)

    println("Dumping old set to Redis...")
    oldFileOrchestrator.dumpToRedis
    println("Dumping new set to Redis...")
    newFileOrchestrator.dumpToRedis

    val retriever = MorphemesRedisRetriever(redis, oldSetRedisKey, newSetRedisKey)

    for ((term, chiScore) <- retriever.byChiSquaredReversed.filter( x => x._1.length >= minLength && x._1.length <= maxLength).take(top))
      println(s"Term: [$term], χ² score $chiScore")

  }
}