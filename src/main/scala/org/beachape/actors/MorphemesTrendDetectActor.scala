package org.beachape.actors

import akka.actor.{ Actor, Props }
import akka.event.Logging
import com.redis._
import org.beachape.analyze.MorphemesRedisRetriever
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import scala.concurrent.duration._

class MorphemesTrendDetectActor(redisPool: RedisClientPool) extends Actor {

  import context.dispatcher

  implicit val timeout = Timeout(600 seconds)

  val morphemeRetrieveRoundRobin = context.actorOf(Props(new MorphemeRedisRetrieverActor(redisPool)).withRouter(RoundRobinRouter(2)), "morphemeRetrievalRouter")

  def receive = {

    case List('detectTrends, (oldSet: RedisKeySet, newSet: RedisKeySet, minOccurrence: Double)) => {

      val zender = sender
      val listOfStoredRankedTrendsKeysFutures = List(
        ask(morphemeRetrieveRoundRobin, (oldSet, minOccurrence)).mapTo[RedisKey],
        ask(morphemeRetrieveRoundRobin, (newSet, minOccurrence)).mapTo[RedisKey])

      val futureListOfStoredRankedTrendsKeys = Future.sequence(listOfStoredRankedTrendsKeysFutures)

      futureListOfStoredRankedTrendsKeys map { storedRankKeyList =>
        storedRankKeyList match {
          case List(olderMorphemesKey: RedisKey, newerMorphemesKey: RedisKey) => {
            zender ! RedisKeySet(olderMorphemesKey, newerMorphemesKey)
          }
          case _ => exit(1)
        }
      }
    }

    case _ => println("MorphemesTrendDetectActor says 'huh???'")
  }


}