package org.beachape.support

import com.redis._
import org.beachape.server.TrendServer
import akka.actor.ActorSystem
import akka.routing.SmallestMailboxRouter
import org.beachape.actors.MainOrchestrator
import scala.concurrent.duration.DurationInt

case class ConsoleServer(redisHost: String = "localhost", redisPort: Int = 6379, redisDb: Int = 7) {

  val redisPool = new RedisClientPool(redisHost, redisPort, database = redisDb)
  private val system = ActorSystem("akanoriSystem")
  private val mainOrchestratorRoundRobin = system.actorOf(MainOrchestrator(redisPool, dropBlacklisted = true, onlyWhitelisted = true, spanInSeconds = 10800, minOccurrence = 5, minLength = 1, maxLength = 10, top = 10).withRouter(SmallestMailboxRouter(3)), "mainOrchestrator")
  private val server = new TrendServer(mainOrchestratorRoundRobin)

  def currentTrendsDefault = server.currentTrendsDefault

  def generateCurrentTrends = mainOrchestratorRoundRobin ! List('generateDefaultTrends)

  def trendsEndingAt(unixEndAtTime: Int, spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int) = server.trendsEndingAt(unixEndAtTime, spanInSeconds, minOccurrence, minLength, maxLength, top)
}