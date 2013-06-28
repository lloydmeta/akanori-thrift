package com.beachape.support

import com.beachape.actors.MainOrchestrator
import com.beachape.server.TrendServer

import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.actor.actorRef2Scala
import akka.routing.SmallestMailboxRouter

/**
 * Helps devlepment by creating a console server...
 * Stub for now
 */
case class ConsoleServer(redisHost: String = "localhost", redisPort: Int = 6379, redisDb: Int = 7) {

  val redisPool = new RedisClientPool(redisHost, redisPort, database = redisDb)
  private val system = ActorSystem("akanoriSystem")
  private val mainOrchestratorRoundRobin = system.actorOf(
    MainOrchestrator(
      redisPool,
      dropBlacklisted = true,
      onlyWhitelisted = true,
      spanInSeconds = 10800,
      minOccurrence = 5,
      minLength = 1,
      maxLength = 10,
      top = 10).withRouter(SmallestMailboxRouter(3)), "mainOrchestrator")
  private val server = new TrendServer(mainOrchestratorRoundRobin)

  def currentTrendsDefault = server.currentTrendsDefault

  def generateCurrentTrends() = mainOrchestratorRoundRobin ! List('generateDefaultTrends)

  def trendsEndingAt(
    unixEndAtTime: Int,
    spanInSeconds: Int,
    minOccurrence: Double,
    minLength: Int,
    maxLength: Int,
    top: Int,
    dropBlacklisted: Boolean,
    onlyWhitelisted: Boolean) = {
    server.trendsEndingAt(
      unixEndAtTime,
      spanInSeconds,
      minOccurrence,
      minLength,
      maxLength,
      top,
      dropBlacklisted,
      onlyWhitelisted)
  }
}