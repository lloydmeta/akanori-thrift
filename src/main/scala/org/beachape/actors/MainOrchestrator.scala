package org.beachape.actors
import akka.actor.Actor
import akka.actor.Props
import scala.concurrent.Future
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

  def receive = {

    case FullFilePathSet(FilePathSet(oldExpectedPath: FilePath, oldObservedSet: FilePath), FilePathSet(newExpectedPath: FilePath, newObservedPath: FilePath)) => {

      println("Lets get cracking")
      println("*****************\n")

      val listOfRedisKeyFutures = List(
          ask(fileToRedisRoundRobin, oldExpectedPath).mapTo[RedisKey],
          ask(fileToRedisRoundRobin, oldObservedSet).mapTo[RedisKey],
          ask(fileToRedisRoundRobin, newExpectedPath).mapTo[RedisKey],
          ask(fileToRedisRoundRobin, newObservedPath).mapTo[RedisKey]
      )
      val futureListOfRedisKeys = Future.sequence(listOfRedisKeyFutures)

      futureListOfRedisKeys map { redisKeysList =>

        val listOfStoredRankedTrendsKeysFutures = redisKeysList.grouped(2).toList map { redisKeysList =>
          redisKeysList match {
            case List(expectedRedisKey: RedisKey, observedRedisKey: RedisKey) =>
              ask(morphemeRetrieveRoundRobin, (RedisKeySet(expectedRedisKey, observedRedisKey), minOccurrence)).mapTo[RedisKey]
            case _ => exit(1)
          }
        }

        val futureListOfStoredRankedTrendsKeys = Future.sequence(listOfStoredRankedTrendsKeysFutures)

        futureListOfStoredRankedTrendsKeys map { storedRankKeyList =>
          storedRankKeyList match {
            case List(olderMorphemesKey: RedisKey, newerMorphemesKey: RedisKey) => {
              self ! RedisKeySet(olderMorphemesKey, newerMorphemesKey)
            }
            case _ => exit(1)
          }

        }
      }

    }

    case redisKeySet: RedisKeySet => {
      morphemeRetrieveRoundRobin ! List('retrieveChiChi, redisKeySet, minOccurrence, minLength, maxLength, top)
    }
    case 'allDone => {
      println("That's all folks!")
      context.system.shutdown()
    }
    case _ => System.exit(1)
  }
}

// Message definitions

sealed case class RedisKey(redisKey: String)
sealed case class RedisKeySet(expectedKey: RedisKey, observedKey: RedisKey)

sealed case class FilePath(filePath: String)
sealed case class FilePathSet(expected: FilePath, observed: FilePath)
sealed case class FullFilePathSet(older: FilePathSet, newer: FilePathSet)