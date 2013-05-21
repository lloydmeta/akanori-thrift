import org.chasen.mecab.{Tagger, Node, MeCab}
import org.beachape.analyze.{Morpheme, MorphemesRedisRetriever, FileMorphemesToRedis}
import com.redis._
import java.io._

object TrendApp {

  val usage = """
      Usage: TrendApp --file-older path --file-newer path [--drop-blacklisted boolean, defaults to true] [--only-whitelisted boolean, defaults to false] [--redis-host address, defaults to localhost] [--redis-db integer, defaults to 0] [--redis-port integer, defaults to 6379]
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
        case _ => ""
      }
    }

    val newFilePath: String = {
      options.get('fileNewer) match {
        case Some(x:String) => x
        case _ => ""
      }
    }

    val onlyWhitelisted: Boolean = {
      options.get('onlyWhitelisted) match {
        case Some(x:Boolean) => x
        case _ => false
      }
    }

    val dropBlacklisted: Boolean = {
      options.get('dropBlacklisted) match {
        case Some(x:Boolean) => x
        case _ => true
      }
    }

    val redisHost = {
      options.get('redisHost) match {
        case Some(x:String) => x
        case _ => "localhost"
      }
    }

    val redisPort = {
      options.get('redisPort) match {
        case Some(x:Int) => x
        case _ => 6379
      }
    }

    val redisDb = {
      options.get('redisDb) match {
        case Some(x:Int) => x
        case _ => 7
      }
    }

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
    println(retriever.byChiSquaredReversed)

  }
}