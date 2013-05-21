import org.chasen.mecab.{Tagger, Node, MeCab}
import org.beachape.analyze.{Morpheme, MorphemesRedisRetriever, FileMorphemesToRedis}
import org.beachape.support.FileWrite.printToFile
import com.redis._
import java.io._

object TrendApp {

  val usage = """
      Usage: TrendApp --file-older path --file-newer path [--redis-host address, defaults to localhost] [--redis-db integer, defaults to 0] [--redis-port integer, defaults to 6379]
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
        case "--redis-host" :: value :: tail =>
                               nextOption(map ++ Map('redisHost -> value), tail)
        case "--redis-port" :: value :: tail =>
                               nextOption(map ++ Map('redisPort -> value.toInt), tail)
        case "--redis-db" :: value :: tail =>
                               nextOption(map ++ Map('redisDb -> value.toInt), tail)
        // case string :: opt2 :: tail if isSwitch(opt2) =>
        //                        nextOption(map ++ Map('infile -> string), list.tail)
        // case string :: Nil =>  nextOption(map ++ Map('infile -> string), list.tail)
        case option :: tail => println("Unknown option "+option)
                               exit(1)
      }
    }

    val options = nextOption(Map(),arglist)

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

    val redis = new RedisClient(redisHost, redisPort)
    redis.select(redisDb)

    val oldSetRedisKey = "trends:older"
    val newSetRedisKey = "trends:newer"

    val oldFileOrchestrator = FileMorphemesToRedis(oldFilePath, redis, oldSetRedisKey)
    val newFileOrchestrator = FileMorphemesToRedis(newFilePath, redis, newSetRedisKey)

    println("Dumping old set to Redis...")
    oldFileOrchestrator.dumpToRedis
    println("Dumping new set to Redis...")
    newFileOrchestrator.dumpToRedis

    val retriever = MorphemesRedisRetriever(redis, oldSetRedisKey, newSetRedisKey)
    println(retriever.byChiSquaredReversed)

  }
}