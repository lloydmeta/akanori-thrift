import org.beachape.analyze.{ MorphemesRedisTracker, MorphemesRedisRetriever, Morpheme, RedisHelper }
import org.beachape.actors.{RedisKey, RedisKeySet}
import org.beachape.testing.Support
import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import org.beachape.analyze.MorphemesRedisRetriever

class MorphemesRedisRetrieverSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with Support {

  val redisPool = new RedisClientPool("localhost", 6379, database = 4)

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