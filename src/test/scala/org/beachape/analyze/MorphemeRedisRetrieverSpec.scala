package org.beachape.analyze

import scala.runtime.ZippedTraversable2.zippedTraversable2ToTraversable

import org.beachape.actors.RedisKey
import org.beachape.actors.RedisKeySet
import org.beachape.testing.Support
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

class MorphemesRedisRetrieverSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with Support {

  val redisPool = new RedisClientPool("localhost", 6379, database = 6)

  val (oldSet: RedisKeySet, newSet: RedisKeySet) = dumpMorphemesToRedis
  val RedisKeySet(oldExpectedKey: RedisKey, oldObservedKey: RedisKey) = oldSet
  val RedisKeySet(newExpectedKey: RedisKey, newObservedKey: RedisKey) = newSet

  val morphemeRedisRetriever = new MorphemesRedisRetriever(redisPool, oldExpectedKey.redisKey, oldObservedKey.redisKey, minScore = Double.NegativeInfinity)

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpMorphemesToRedis
  }

  describe("#forEachPageOfObservedTermsWithScores") {

    it("should yield for every page found") {
      morphemeRedisRetriever.forEachPageOfObservedTermsWithScores() { _ => true }.forall(list => list.forall(x => x == true)) should be(true)
    }

  }

  describe("#getOldScoreForTerm") {

    it("should get me the right number") {
      morphemeRedisRetriever.getExpectedScoreForTerm("b") should be(4)
    }

  }

  describe("#getNewScoreForTerm") {

    it("should get me the right number") {
      morphemeRedisRetriever.getObservedScoreForTerm("b") should be(6)
    }

  }

  describe("#totalExpectedSetMorphemesScore") {

    it("should get me the right number") {
      morphemeRedisRetriever.totalExpectedSetMorphemesScore should be(21.0)
    }

  }

  describe("#totalObservedSetMorphemesScore") {

    it("should get me the right number") {
      morphemeRedisRetriever.totalObservedSetMorphemesScore should be(26.0)
    }

  }

  describe("#observedZCard") {

    it("should get me the right number") {
      morphemeRedisRetriever.observedZCard should be(6)
    }

  }

  describe("#chiSquaredForTerm") {

    it("should get me the right score with observedScore provided") {
      morphemeRedisRetriever.chiSquaredForTerm("b", 6, 21, 26) should be(0.8078073877479973)
    }

    it("should get me the right score without observedScore provided") {
      morphemeRedisRetriever.chiSquaredForTerm("b", 21, 26) should be(0.8078073877479973)
    }

  }
}