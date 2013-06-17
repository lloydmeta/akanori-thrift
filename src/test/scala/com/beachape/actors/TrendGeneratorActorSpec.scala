package com.beachape.actors

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.beachape.testing.Support
import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit

class TrendGeneratorActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout
  with Support {

  val redisPool = new RedisClientPool("localhost", 6379, database = 5)

  val (oldSet: RedisKeySet, newSet: RedisKeySet) = dumpMorphemesToRedis
  val RedisKeySet(oldExpectedKey: RedisKey, oldObservedKey: RedisKey) = oldSet
  val RedisKeySet(newExpectedKey: RedisKey, newObservedKey: RedisKey) = newSet

  val trendGeneratorActorRef = TestActorRef(new TrendGeneratorActor(redisPool))
  val trendGeneratorActor = trendGeneratorActorRef.underlyingActor

  val map = dumpStringsToRedisStoredStringSet
  val unixStart = map.getOrElse('unixStartTime, 0)
  val unixEnd = map.getOrElse('unixEndTime, 0)
  val span = map.getOrElse('span, 0)

  //  Map(
  //    //oldExpectedStrings
  //    (unixStartTime + 1) -> Map(
  //      "笹子" -> 2,
  //      "トンネル" -> 1,
  //      "設計" -> 2,
  //      "見落とし" -> 2,
  //      "ーーー" -> 3,
  //      "殺す" -> 3),
  //    //oldObservedStrings
  //    (unixStartTime + span + 1) -> Map(
  //      "笹子" -> 3,
  //      "トンネル" -> 4,
  //      "設計" -> 3,
  //      "見落とし" -> 1,
  //      "ーーー" -> 3,
  //      "殺す" -> 9),
  //    //newExpectededStrings
  //    (unixEndTime - 2 * span + 1) -> Map(
  //      "笹子" -> 2,
  //      "トンネル" -> 1,
  //      "設計" -> 4,
  //      "見落とし" -> 5,
  //      "坂本" -> 2,
  //      "ーーー" -> 2,
  //      "殺す" -> 1),
  //    //newObservedStrings
  //    (unixEndTime - span + 1) -> Map(
  //      "笹子" -> 4,
  //      "トンネル" -> 6,
  //      "設計" -> 4,
  //      "見落とし" -> 9,
  //      "坂本" -> 10,
  //      "ーーー" -> 6,
  //      "殺す" -> 9))

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpStringsToRedisStoredStringSet
  }

  describe("sending a GenerateAndCacheTrendsFor with given parameters") {

    it("should respond with an expected trend with zero filtering") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 0.0, 1, 20, 20, false, false)
      expectMsg(validTrendNoFiltering)
    }

    it("should respond with an expected trend with blacklist filtering") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 0.0, 1, 20, 20, true, false)
      expectMsg(validTrendDropBlacklisted)
    }

    it("should respond with an expected trend with whitelist filtering") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 0.0, 1, 20, 20, false, true)
      expectMsg(validTrendOnlyWhitelisted)
    }

    it("should respond with an expected trend when using tweaked minOccurrence") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 6.0, 1, 20, 20, false, false)
      expectMsg(validTrendWithMinOccurrence)
    }

    it("should respond with an expected trend when using tweaked minLength") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 0.0, 3, 20, 20, false, false)
      expectMsg(validTrendWithMinLength)
    }

    it("should respond with an expected trend when using tweaked maxLength") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 0.0, 1, 3, 20, false, false)
      expectMsg(validTrendWithMaxLength)
    }

    it("should respond with an expected trend when using tweaked top") {
      trendGeneratorActorRef ! GenerateAndCacheTrendsFor(RedisKey("test:something"), unixEnd, span, 0.0, 1, 10, 2, false, false)
      expectMsg(validTrendWithTop)
    }

  }
}