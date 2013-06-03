package org.beachape.actors

import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{ TestActorRef, TestKit, ImplicitSender, DefaultTimeout }
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import org.beachape.testing.Support
import scala.util.{ Try, Success, Failure }

class TrendGeneratorActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout
  with Support{

  val redisPool = new RedisClientPool("localhost", 6379, database = 5)

  val (oldSet: RedisKeySet, newSet: RedisKeySet) = dumpMorphemesToRedis
  val RedisKeySet(oldExpectedKey: RedisKey, oldObservedKey: RedisKey) = oldSet
  val RedisKeySet(newExpectedKey: RedisKey, newObservedKey: RedisKey) = newSet

  val trendGeneratorActorRef = TestActorRef(new TrendGeneratorActor(redisPool, false, false))
  val trendGeneratorActor = trendGeneratorActorRef.underlyingActor

  val map = dumpStringsToRedisStoredStringSet
  val unixStart = map.getOrElse('unixStartTime, 0)
  val unixEnd = map.getOrElse('unixEndTime, 0)
  val span = map.getOrElse('span, 0)

  val validTrend = List(
    ("見落とし",9739.509318505337),
    ("トンネル",6670.792725392501),
    ("坂本",367.3888435418149))


  // Map(
  //   //oldExpectedStrings
  //   (unixStartTime + 1) -> Map(
  //   "笹子" -> 2,
  //   "トンネル" -> 1,
  //   "設計" -> 2,
  //   "見落とし" -> 2),
  //   //oldObservedStrings
  //   (unixStartTime + span + 1) -> Map(
  //   "笹子" -> 3,
  //   "トンネル" -> 4,
  //   "設計" -> 3,
  //   "見落とし" -> 1),
  //   //newExpectededStrings
  //    (unixEndTime - span - 1) -> Map(
  //   "笹子" -> 2,
  //   "トンネル" -> 1,
  //   "設計" -> 4,
  //   "見落とし" -> 5,
  //   "坂本" -> 2),
  //   //newObservedStrings
  //   (unixEndTime - span + 1) -> Map(
  //   "笹子" -> 4,
  //   "トンネル" -> 6,
  //   "設計" -> 4,
  //   "見落とし" -> 9,
  //   "坂本" -> 10)
  //   )

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpStringsToRedisStoredStringSet
  }

  describe("sending a message to List('generateTrendsFor....)") {

    it("should respond with an expected trend") {
      trendGeneratorActorRef ! List('generateTrendsFor, (RedisKey("test:something"), unixEnd, span, 0.0, 1, 20, 20))
      expectMsg(validTrend)
    }

  }
}