package org.beachape.actors

import akka.actor.{ Actor, Props }
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import akka.event.Logging
import akka.routing.RoundRobinRouter
import com.redis._
import akka.util.Timeout
import scala.concurrent.duration._

object MainOrchestrator {
  def apply(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int): Props = Props(new MainOrchestrator(redisPool, dropBlacklisted, onlyWhitelisted, minOccurrence, minLength, maxLength, top))
}

class MainOrchestrator(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(600 seconds)

  val fileToRedisRoundRobin = context.actorOf(Props(new FileToRedisActor(redisPool, dropBlacklisted, onlyWhitelisted)).withRouter(RoundRobinRouter(4)), "fileRouter")
  val morphemeRetrieveRoundRobin = context.actorOf(Props(new MorphemeRedisRetrieverActor(redisPool)).withRouter(RoundRobinRouter(2)), "morphemeRetrievalRouter")
  val morphemeAnalyzerRoundRobin = context.actorOf(Props(new MorphemesAnalyzerActor(redisPool)).withRouter(RoundRobinRouter(10)), "mainOrchestartorMorphemesAnalyzerRoundRobin")
  val stringToRedisRoundRobin = context.actorOf(Props(new StringToRedisActor(redisPool)).withRouter(RoundRobinRouter(10)), "mainOrchestartorStringToRedisRoundRobin")
  val redisStringSetToMorphemesOrchestrator = context.actorOf(Props(new RedisStringSetToMorphemesOrchestrator(redisPool)))
  val morphemesTrendDetectRoundRobin = context.actorOf(Props(new MorphemesTrendDetectActor(redisPool)).withRouter(RoundRobinRouter(2)), "morphemesTrendDetectRoundRobin")

  def receive = {

    case FullFilePathSet(FilePathSet(oldExpectedPath: FilePath, oldObservedSet: FilePath), FilePathSet(newExpectedPath: FilePath, newObservedPath: FilePath)) => {

      println("Lets get cracking")
      println("*****************\n")

      val listOfRedisKeyFutures = List(
        ask(fileToRedisRoundRobin, oldExpectedPath).mapTo[RedisKey],
        ask(fileToRedisRoundRobin, oldObservedSet).mapTo[RedisKey],
        ask(fileToRedisRoundRobin, newExpectedPath).mapTo[RedisKey],
        ask(fileToRedisRoundRobin, newObservedPath).mapTo[RedisKey])
      val futureListOfRedisKeys = Future.sequence(listOfRedisKeyFutures)
      futureListOfRedisKeys map { redisKeysList =>
        redisKeysList match {
          case List(oldExpectedKey: RedisKey, oldObservedKey: RedisKey, newExpectedKey: RedisKey, newObservedKey: RedisKey) => {
            self ! List('detectTrends, RedisKeySet(oldExpectedKey, oldObservedKey), RedisKeySet(newExpectedKey, newObservedKey))
          }
          case _ => exit(1)
        }
      }
    }

    case List('detectTrends, oldSet: RedisKeySet, newSet: RedisKeySet) => {
      val listOfStoredRankedTrendsKeysFutures = List(
        ask(morphemeRetrieveRoundRobin, (oldSet, minOccurrence)).mapTo[RedisKey],
        ask(morphemeRetrieveRoundRobin, (newSet, minOccurrence)).mapTo[RedisKey])

      val futureListOfStoredRankedTrendsKeys = Future.sequence(listOfStoredRankedTrendsKeysFutures)

      futureListOfStoredRankedTrendsKeys map { storedRankKeyList =>
        storedRankKeyList match {
          case List(olderMorphemesKey: RedisKey, newerMorphemesKey: RedisKey) => {
            self ! List('retrieveChiChi, RedisKeySet(olderMorphemesKey, newerMorphemesKey))
          }
          case _ => exit(1)
        }
      }
    }

    case List('retrieveChiChi, redisKeySet: RedisKeySet) => {
      redisKeySet match {
        case RedisKeySet(expectedKey, observedKey) => {
          redisPool.withClient { redis =>
            redis.hmset(hashOfLatestTrendKeysKey, Map("expected" -> expectedKey.redisKey, "observed" -> observedKey.redisKey))
          }
        }
        case _ =>
      }

      morphemeRetrieveRoundRobin ! List('printChiChi, redisKeySet, minOccurrence, minLength, maxLength, top)
    }

    case 'allDone => {
      println("That's all folks!")
      //      context.system.shutdown()
    }

    case 'getTrendsDefault => {
      val zender = sender
      val listOfReverseSortedTermsAndScoresFuture = morphemeRetrieveRoundRobin ? List('retrieveChiChi, latestTrendsRedisKeySet, minOccurrence, minLength, maxLength, top)
      listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
        zender ! listOfReverseSortedTermsAndScores
      }
    }

    case List('getTrends, (callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int)) => {
      val zender = sender
      val listOfReverseSortedTermsAndScoresFuture = morphemeRetrieveRoundRobin ? List('retrieveChiChi, latestTrendsRedisKeySet, callMinOccurrence, callMinLength, callMaxLength, callTop)
      listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
        zender ! listOfReverseSortedTermsAndScores
      }
    }

    case message @ List('storeString, (stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int)) => {
      stringToRedisRoundRobin ! message
    }

    case message @ List('getTrendsEndingAt, (unixEndAtTime: Int, spanInSeconds: Int, callMinOccurrence: Double, callMinLength: Int, callMaxLength: Int, callTop: Int)) => {

      val zender = sender
      val listOfRedisKeysFuture = ask(redisStringSetToMorphemesOrchestrator, List('generateTrendsFor, (unixEndAtTime, spanInSeconds, dropBlacklisted: Boolean, onlyWhitelisted: Boolean)))

      listOfRedisKeysFuture map { listOfRedisKeys =>
        listOfRedisKeys match {
          case List(oldSet: RedisKeySet, newSet: RedisKeySet) => {
            val oldNewMorphemesSetKeys = ask(morphemesTrendDetectRoundRobin, List('detectTrends, (oldSet, newSet, callMinOccurrence)))
            oldNewMorphemesSetKeys map { keySet =>
              keySet match {
                case newlyGeneratedSet:RedisKeySet => {
                  val listOfReverseSortedTermsAndScoresFuture = morphemeRetrieveRoundRobin ? List('retrieveChiChi, newlyGeneratedSet, callMinOccurrence, callMinLength, callMaxLength, callTop)
                  listOfReverseSortedTermsAndScoresFuture map { listOfReverseSortedTermsAndScores =>
                    zender ! listOfReverseSortedTermsAndScores
                  }
                }
                case _ => print("daaamn")
              }
            }
          }
          case _ =>
        }
      }

    }

    case _ => System.exit(1)
  }

  def hashOfLatestTrendKeysKey = "trends:latest_keys"

  def latestTrendsRedisKeySet: RedisKeySet = {
    redisPool.withClient { redis =>
      redis.hmget(hashOfLatestTrendKeysKey, "expected", "observed")
    } match {
      case Some(x: Map[String, String]) if (x.contains("expected") && x.contains("observed")) =>
        RedisKeySet(RedisKey(x.getOrElse("expected", "")), RedisKey(x.getOrElse("observed", "")))
      case _ => RedisKeySet(RedisKey(""), RedisKey(""))
    }
  }
}