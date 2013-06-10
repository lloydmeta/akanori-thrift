package org.beachape.server

import org.beachape.actors.MainOrchestrator
import org.beachape.testing.Support
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.routing.SmallestMailboxRouter
import trendServer.gen.TrendResult

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

class TrendServerSpec
  extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with Support {

  val redisPool = new RedisClientPool("localhost", 6379, database = 9)
  val systemTest = ActorSystem("akanoriSystem")
  val mainOrchestratorRoundRobin = systemTest.actorOf(MainOrchestrator(redisPool, dropBlacklisted = true, onlyWhitelisted = true, spanInSeconds = 10800, minOccurrence = 5, minLength = 1, maxLength = 10, top = 10).withRouter(SmallestMailboxRouter(3)), "mainOrchestrator")
  val server = new TrendServer(mainOrchestratorRoundRobin)

  val map = dumpStringsToRedisStoredStringSet
  val unixStart = map.getOrElse('unixStartTime, 0)
  val unixEnd = map.getOrElse('unixEndTime, 0)
  val span = map.getOrElse('span, 0)

  before {
    redisPool.withClient(redis => redis.flushdb)
  }

  describe("#trendsEndingAt") {

    val trendListConverted: java.util.List[TrendResult] = ListBuffer(validTrendNoFiltering map { x => new TrendResult(x._1, x._2) }: _*)

    it("should return the proper result when there are strings in Redis stored set") {
      dumpStringsToRedisStoredStringSet
      server.trendsEndingAt(unixEnd, span, 0.0, 1, 20, 20, false, false) should be(trendListConverted)
    }

    it("should return an empty array when there is nothing in the stored set") {
      server.trendsEndingAt(unixEnd, span, 0.0, 1, 20, 20, false, false) should be('empty)
    }

  }
}