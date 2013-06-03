package org.beachape.analyze

import org.beachape.actors.{ RedisKey, RedisKeySet }
import org.beachape.testing.Support
import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers

class MorphemesRedisRetrieverSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with Support {

  val redisPool = new RedisClientPool("localhost", 6379, database = 6)

  val (oldSet: RedisKeySet, newSet: RedisKeySet) = dumpMorphemesToRedis
  val RedisKeySet(oldExpectedKey: RedisKey, oldObservedKey: RedisKey) = oldSet
  val RedisKeySet(newExpectedKey: RedisKey, newObservedKey: RedisKey) = newSet

  val morphemeRedisRetriever = new MorphemesRedisRetriever(redisPool, oldExpectedKey.redisKey, oldObservedKey.redisKey, minScore = Double.NegativeInfinity)
  val morphemeRedisRetrieverEmpty = new MorphemesRedisRetriever(redisPool, oldExpectedKey.redisKey + "fake", oldExpectedKey.redisKey + "fake", minScore = Double.NegativeInfinity)

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpMorphemesToRedis
  }

  describe("#byChiSquared") {

    it("should be empty for a MorphemesRedisRetriever given empty keys") {
      morphemeRedisRetrieverEmpty.byChiSquared().isEmpty should be(true)
    }

    it("should not be empty for a MorphemesRedisRetriever with redis keys that contain morphemes with scores") {
      morphemeRedisRetriever.byChiSquared().isEmpty should be(false)
    }

    describe("results checking. Consider this a way of making sure things are still working WRT the math") {

      val morphemesByChiSquared = morphemeRedisRetriever.byChiSquared()
      // based on org.beachape.testing.Support trait
      val properResults = List(("c", 11.040408468934922), ("b", 0.8078073877479973), ("e", 0.006510833623390149))

      it("should give me properly ranked results") {
        val zippedList = (morphemesByChiSquared, morphemesByChiSquared.tail).zipped.toList
        for (((_: String, priorScore: Double), (_: String, afterScore: Double)) <- zippedList) {
          priorScore should be > afterScore
        }
      }

      it("should give me proper morphemes at each result") {
        val rankedMorphemesProper = for ((term: String, _: Double) <- properResults) yield term
        val zippedProperAndResults = (rankedMorphemesProper, morphemesByChiSquared).zipped.toList
        for ((properMorpheme: String, (rankedMorpheme: String, _: Double)) <- zippedProperAndResults) {
          rankedMorpheme should be(properMorpheme)
        }
      }

      it("should give me proper scores at each result") {
        val rankedScoresProper = for ((_: String, score: Double) <- properResults) yield score
        val zippedProperAndResults = (rankedScoresProper, morphemesByChiSquared).zipped.toList
        for ((score: Double, (_: String, resultScore: Double)) <- zippedProperAndResults) {
          roughRound(resultScore, 0.00000001) should be(roughRound(score, 0.00000001))
        }
      }

    }

  }

  describe("#generateAndStoreChiSquared") {

    it("should return a string that can be used as a key") {
      val storedChiSquared = morphemeRedisRetriever.generateAndStoreChiSquared
      redisPool.withClient(redis =>
        redis.exists(storedChiSquared)) should be(true)
    }

    describe("should return a key and the key itself") {

      it("should point to a value in Redis that is a zSet (responds to zCard with a number greater than 0)") {
        val storedChiSquared = morphemeRedisRetriever.generateAndStoreChiSquared
        zcardOfRedisKey(storedChiSquared) should be > 0
      }

    }

  }
}