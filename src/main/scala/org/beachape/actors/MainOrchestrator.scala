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
  val fileToRedisRoundRobin = context.actorOf(Props(new FileToRedisActor(redisPool, dropBlacklisted, onlyWhitelisted)).withRouter(RoundRobinRouter(2)), "fileRouter")
  val morphemeRetrieveRoundRobin = context.actorOf(Props(new MorphemeRedisRetrieverActor(redisPool)).withRouter(RoundRobinRouter(2)), "morphemeRetrievalRouter")

  def receive = {

    case FullFilePathSet(oldSet: FilePathSet, newSet: FilePathSet) => {

      val listOfRedisKeySetFutures = List(
          ask(fileToRedisRoundRobin, oldSet).mapTo[RedisKeySet],
          ask(fileToRedisRoundRobin, newSet).mapTo[RedisKeySet]
      )
      val futureListOfRedisKeySets = Future.sequence(listOfRedisKeySetFutures)

      futureListOfRedisKeySets map { redisKeySetlist =>
        val listOfStoredRankedTrendsKeysFutures = redisKeySetlist map { redisKeySet =>
          ask(morphemeRetrieveRoundRobin, (redisKeySet, minOccurrence)).mapTo[RedisKey]
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