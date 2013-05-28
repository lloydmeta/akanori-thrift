package org.beachape.actors

import akka.actor.{ Actor, Props }
import akka.event.Logging
import com.redis._
import akka.pattern.ask
import akka.routing.SmallestMailboxRouter
import scala.concurrent.{ Await, Future }
import akka.util.Timeout
import scala.concurrent.duration._
import com.github.nscala_time.time.Imports._

class TrendGeneratorActor(val redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) extends Actor with RedisStorageHelper {

  import context.dispatcher
  implicit val timeout = Timeout(DurationInt(600) seconds)

  val morphemeRetrieveRoundRobin = context.actorOf(Props(new MorphemeRedisRetrieverActor(redisPool)).withRouter(SmallestMailboxRouter(2)), "morphemeRetrievalRouter")
  val redisStringSetToMorphemesOrchestrator = context.actorOf(Props(new RedisStringSetToMorphemesOrchestrator(redisPool)))
  val morphemesTrendDetectRoundRobin = context.actorOf(Props(new MorphemesTrendDetectActor(redisPool)).withRouter(SmallestMailboxRouter(2)), "morphemesTrendDetectRoundRobin")

  def receive = {

    case List('generateTrendsFor, (redisCacheKey: RedisKey, unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int)) => {

      val zender = sender
      val listOfRedisKeysFuture = ask(redisStringSetToMorphemesOrchestrator, List('generateTrendsFor, (unixEndAtTime, spanInSeconds, dropBlacklisted: Boolean, onlyWhitelisted: Boolean)))

      listOfRedisKeysFuture map { listOfRedisKeys =>
        listOfRedisKeys match {
          case List(oldSet: RedisKeySet, newSet: RedisKeySet) => {
            val oldNewMorphemesSetKeys = ask(morphemesTrendDetectRoundRobin, List('detectTrends, (oldSet, newSet, callMinOccurrence)))
            oldNewMorphemesSetKeys map { keySet =>
              keySet match {
                case newlyGeneratedSet: RedisKeySet => {
                  val listOfReverseSortedTermsAndScoresFuture = ask(morphemeRetrieveRoundRobin, List('retrieveChiChi, newlyGeneratedSet, callMinOccurrence, callMinLength, callMaxLength, callTop)).mapTo[List[(String, Double)]]
                  listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
                    cacheTrendsAtScore(redisCacheKey, listOfReverseSortedTermsAndScores)
                    zender ! listOfReverseSortedTermsAndScores
                  }
                }
                case _ => throw new Exception("TrendGeneratorActor failed to generate trends")
              }
            }
          }
          case _ => throw new Exception("TrendGeneratorActor did not receive proper Redis keys pointing to morphemes")
        }
      }
    }

    case _ => println("TrendGeneratorActor says 'huh?'")

  }

  def cacheTrendsAtScore(cacheKey: RedisKey, listOfReverseSortedTermsAndScores: List[(String, Double)]) = {
    redisPool.withClient { redis =>
      {
        for ((term: String, score: Double) <- listOfReverseSortedTermsAndScores) {
         redis.zincrby(cacheKey.redisKey, score, term)
        }
        redis.pexpire(cacheKey.redisKey, RichInt(15).minute.millis.toInt)
      }
    }
  }
}