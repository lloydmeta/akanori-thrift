package com.beachape.actors

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import com.beachape.helpers.RedisStorageHelper
import com.github.nscala_time.time.Imports.RichInt
import com.redis.RedisClientPool

import akka.actor.Actor
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import akka.util.Timeout

/**
 * Companion object that houses the factory apply
 * method that returns the Props required to instantiate
 * a [[com.beachape.actors.RedisStringSetToMorphemesActor]]
 */
object RedisStringSetToMorphemesActor {

  /**
   * Returns the Props required to spawn an instance of RedisStringSetToMorphemesActor
   *
   * @param redisPool a RedisClientPool that will be used by the actor
   */
  def apply(redisPool: RedisClientPool) = Props(new RedisStringSetToMorphemesActor(redisPool))
}

/**
 * Actor that looks through the stored strings sorted set and counts
 * the morphemes in the strings for that span, then caches the counts
 * per morpheme in a given redis sorted set.
 *
 * Receives generateMorphemesForSpan messages and
 * based on the unixTimeSpan, dropBlacklisted and onlyWhitelisted
 * options, and cache redisKey passed in, looks through the standard
 * sorted set where strings are stored, breaks things out into morphemes
 * and stores the count of each morpheme in that time period the cacheKey
 * as a sorted set
 *
 * Should be instantiated using the props returned by the companion object.
 */
class RedisStringSetToMorphemesActor(val redisPool: RedisClientPool) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600).seconds)

  val morphemeAnalyzerRoundRobin = context.actorOf(
    MorphemesAnalyzerActor(redisPool).withRouter(SmallestMailboxRouter(30)),
    "redisStringSetToMorphemesActorMorphemesAnalyzerRoundRobin")

  def receive = {

    // Given a Unix timespan, get the Redis strings out of the storageKey and tally up
    // the morphemes
    case message: GenerateMorphemesForSpan => {
      val zender = sender
      val redisKey = RedisKey(redisKeyForUnixTimeSpanWithOptions(
        message.unixTimeSpan,
        message.dropBlacklisted,
        message.onlyWhitelisted))
      if (cachedKeyExists(redisKey)) {
        zender ! redisKey
      } else {
        val analyzeAndDumpResultsListMaybe = mapEachPagedListOfTermsInUnixTimeSpan(
          message.unixTimeSpan)(dumpListOfStringsToMorphemes(
          _: Option[List[String]],
          redisKey,
          message.dropBlacklisted,
          message.onlyWhitelisted))
        analyzeAndDumpResultsListMaybe.map {
          case x:List[Option[Boolean]] if x.forall(_ == Some(true)) => {
            setExpiryOnRedisKey(redisKey, (RichInt(15).minutes.millis / 1000).toInt)
            zender ! redisKey
          }
          case _ => throw new Exception("morphemeAnalyzerRoundRobin failed to generate morphemes for that timespan")
        }
      }
    }

    case _ => println("RedisStringSetToMorphemesActor says 'huh?'")
  }

  /**
   * Returns a list of the results of a callback called on each page of
   * strings in the Redis sorted set given a unixTimeSpan
   * (see [[com.beachape.actors.Messages]]), and an optional count,
   *
   * @param unixTimeSpan see [[com.beachape.actors.Messages]]
   * @param count the number of strings per page to return (optional, defaults to 300)
   * @param callBack a function that takes a list of strings and returns a result
   */
  def mapEachPagedListOfTermsInUnixTimeSpan[A](unixTimeSpan: UnixTimeSpan, count: Int = 300)(callBack: Option[List[String]] => A): Option[List[A]] = {
    Some(
      (for (offSet <- (0 to countOfTermsInSpan(unixTimeSpan) by count)) yield {
        callBack(listOfTermsInUnixTimeSpan(unixTimeSpan, Some(offSet, count)))}).toList)
  }

  /**
   * Returns true or false based on whether an attempt to
   * analyze a List[String] into morphemes and store them in a cache
   * Redis sorted key was successful (true) or not (false)
   *
   * @param listOfTerms A list of Strings that need to be analyzed to morphemes
   *  and the morphemes counted and cached
   * @param redisKey where the morpheme counts should be stored (proper message type)
   * @param dropBlacklisted drop blacklisted terms (see [[com.beachape.analyze.Morpheme]])
   * @param onlyWhitelisted keep only whitelisted terms (see [[com.beachape.analyze.Morpheme]])
   */
  def dumpListOfStringsToMorphemes(
                                    someListOfTerms: Option[List[String]],
                                    redisKey: RedisKey,
                                    dropBlacklisted: Boolean,
                                    onlyWhitelisted: Boolean): Option[Boolean] = {
    someListOfTerms.map{ listOfTerms =>

          
      val (listOfStringsOne: List[String], listOfStringsTwo: List[String]) = listOfTerms.splitAt(listOfTerms.length / 2)
      val futureStringOneDump = (morphemeAnalyzerRoundRobin ? AnalyseAndStoreInRedisKey(
        listOfStringsOne.mkString(sys.props("line.separator")),
        redisKey,
        dropBlacklisted,
        onlyWhitelisted)).mapTo[Boolean]
      val futureStringTwoDump = (morphemeAnalyzerRoundRobin ? AnalyseAndStoreInRedisKey(
        listOfStringsTwo.mkString(sys.props("line.separator")),
        redisKey,
        dropBlacklisted,
        onlyWhitelisted)).mapTo[Boolean]

      Await.result(futureStringOneDump, timeout.duration).asInstanceOf[Boolean] &&
        Await.result(futureStringTwoDump, timeout.duration).asInstanceOf[Boolean]
    }
  }

  private def countOfTermsInSpan(unixTimeSpan: UnixTimeSpan): Int = {
    redisPool.withClient { redis =>
      redis.zcount(storedStringsSetKey, unixTimeSpan.start.time.toDouble, unixTimeSpan.end.time.toDouble, true, true) match {
        case Some(x: Long) => x.toInt
        case None => 0
      }
    }
  }

  private def listOfTermsInUnixTimeSpan(timeSpan: UnixTimeSpan, limit: Option[(Int, Int)] = None): Option[List[String]] = {
    redisPool.withClient { redis =>
      redis.zrangebyscore(
        storedStringsSetKey,
        timeSpan.start.time.toDouble,
        true, timeSpan.end.time.toDouble,
        true, limit).map{ list =>
        list.map(storedStringToString(_))
      }
    }
  }

  private def redisKeyForUnixTimeSpanWithOptions(timeSpan: UnixTimeSpan, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) =
    f"morphemes_set:$timeSpan%s-dropBlacklisted$dropBlacklisted%b-onlyWhitelisted$onlyWhitelisted%b"

}