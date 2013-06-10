package org.beachape.actors

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.beachape.support.RichRange.range2RichRange

import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

class RedisStringSetToMorphemesActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600).seconds)

  val morphemeAnalyzerRoundRobin = context.actorOf(Props(new MorphemesAnalyzerActor(redisPool)).withRouter(SmallestMailboxRouter(3)), "redisStringSetToMorphemesMorphemesAnalyzerRoundRobin")

  def receive = {

    // Given a Unix timespan, get the Redis strings out of the storageKey and tally up
    // the morphemes
    case message: GenerateMorphemesForSpan => {
      val zender = sender
      val redisKey = RedisKey(redisKeyForUnixTimeSpanWithOptions(message.unixTimeSpan, message.dropBlacklisted, message.onlyWhitelisted))
      if (cachedKeyExists(redisKey)) {
        zender ! redisKey
      } else {
        val analyzeAndDumpResultsList: List[Boolean] = forEachPagedListOfTermsInUnixTimeSpan(message.unixTimeSpan)(dumpListOfStringsToMorphemes(_: List[String], redisKey, message.dropBlacklisted, message.onlyWhitelisted))
        analyzeAndDumpResultsList match {
          case x: List[Boolean] if x.forall(_ == true) => {
            setExpiryOnRedisKey(redisKey, (RichInt(15).minutes.millis / 1000).toInt)
            zender ! redisKey
          }
          case _ => throw new Exception("morphemeAnalyzerRoundRobin failed to generate morphemes for that timespan")
        }

      }
    }

    case _ => println("RedisStringSetToMorphemesActor says 'huh?'")
  }

  def listOfTermsInUnixTimeSpan(timeSpan: UnixTimeSpan, limit: Option[(Int, Int)] = None) = {
    redisPool.withClient { redis =>
      redis.zrangebyscore(storedStringsSetKey, timeSpan.start.time.toDouble, true, timeSpan.end.time.toDouble, true, limit) match {
        case Some(x: List[String]) => {
          x map (storedStringToString(_))
        }
        case _ => throw new Exception("morphemeAnalyzerRoundRobin couldn't retrieve strings for that timespan")
      }
    }
  }

  def forEachPagedListOfTermsInUnixTimeSpan[A](unixTimeSpan: UnixTimeSpan, count: Int = 300)(callBack: List[String] => A): List[A] = {
    (for (offSet <- (0 to countOfTermsInSpan(unixTimeSpan) by count)) yield {
      callBack(listOfTermsInUnixTimeSpan(unixTimeSpan, Some(offSet, count)))
    })(collection.breakOut)
  }

  def dumpListOfStringsToMorphemes(listOfTerms: List[String], redisKey: RedisKey, dropBlacklisted: Boolean, onlyWhitelisted: Boolean): Boolean = {
    // Split into two
    val (listOfStringsOne: List[String], listOfStringsTwo: List[String]) = listOfTerms.splitAt(listOfTerms.length / 2)
    val futureStringOneDump = (morphemeAnalyzerRoundRobin ? AnalyseAndStoreInRedisKey(listOfStringsOne.mkString(sys.props("line.separator")), redisKey, dropBlacklisted, onlyWhitelisted)).mapTo[Boolean]
    val futureStringTwoDump = (morphemeAnalyzerRoundRobin ? AnalyseAndStoreInRedisKey(listOfStringsTwo.mkString(sys.props("line.separator")), redisKey, dropBlacklisted, onlyWhitelisted)).mapTo[Boolean]

    Await.result(futureStringOneDump, timeout.duration).asInstanceOf[Boolean] &&
      Await.result(futureStringTwoDump, timeout.duration).asInstanceOf[Boolean]
  }

  def countOfTermsInSpan(unixTimeSpan: UnixTimeSpan) = {
    redisPool.withClient { redis =>
      redis.zcount(storedStringsSetKey, unixTimeSpan.start.time.toDouble, unixTimeSpan.end.time.toDouble, true, true) match {
        case Some(x: Long) => x.toInt
        case None => 0
      }
    }
  }

  def redisKeyForUnixTimeSpanWithOptions(timeSpan: UnixTimeSpan, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) =
    f"trends:$timeSpan%s"

}