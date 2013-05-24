package org.beachape.actors
import akka.actor.Actor
import akka.event.Logging
import com.redis._
import org.beachape.analyze.FileMorphemesToRedis
import akka.actor.Props
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import scala.io.Source
import scala.concurrent.{Await, Future}
import akka.util.Timeout
import scala.concurrent.duration._


class FileToRedisActor(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) extends Actor {

  import context.dispatcher
  implicit val timeout = Timeout(600 seconds)

  val morphemeAnalyzerRoundRobin = context.actorOf(Props(new MorphemesAnalyzerActor(redisPool)).withRouter(RoundRobinRouter(4)), "morphemesAnalyzerRoundRobin")

  def receive = {

    case FilePath(filePath) => {
      val redisKey = redisKeyForPath(filePath)

      val listOfAnalyzeAndDumpFutures = Source.fromFile(filePath).getLines().flatMap(line =>
        ask(morphemeAnalyzerRoundRobin, List('dumpMorphemesToRedis, redisKey, line, dropBlacklisted, onlyWhitelisted))
      ).toList

      val futureListOfAnalyzeAndDumpResults = Future.sequence(listOfAnalyzeAndDumpFutures)

      futureListOfAnalyzeAndDumpResults map {analyzeAndDumpResultsList =>
        analyzeAndDumpResultsList match {
          case x:List[Boolean] if x.forall(_ == true) => sender ! RedisKey(redisKey)
          case _ => exit(0)
        }
      }
    }

    case _ => println("huh?")
  }

  def redisKeyForPath(path: String) = f"trends:$path%s"
}