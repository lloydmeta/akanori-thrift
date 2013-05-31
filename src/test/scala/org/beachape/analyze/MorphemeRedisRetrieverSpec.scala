import org.beachape.analyze.{ MorphemesRedisTracker, MorphemesRedisRetriever, Morpheme, RedisHelper }
import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import org.beachape.analyze.MorphemesRedisRetriever

class MorphemesRedisRetrieverSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter {

  val redisPool = new RedisClientPool("localhost", 6379, database = 4)
  val listOfMorphemes = Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か") ::: Morpheme.stringToMorphemes("米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事") ::: Morpheme.stringToMorphemes("朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")
  val listOfMorphemes2 = Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か2") ::: Morpheme.stringToMorphemes("米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事2") ::: Morpheme.stringToMorphemes("朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ2") ::: Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か2") ::: Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か2") ::: Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か2")
  val redisKey = "test:RedisKeyForMorphemesTracker"
  val redisKey2 = "test:RedisKeyForMorphemesTracker2"
  val morphemeRedisTracker = new MorphemesRedisTracker(listOfMorphemes, redisPool, redisKey)
  val morphemeRedisTracker2 = new MorphemesRedisTracker(listOfMorphemes2, redisPool, redisKey2)

  val morphemeRedisRetriever = new MorphemesRedisRetriever(redisPool, redisKey, redisKey2, minScore = Double.NegativeInfinity)
  val morphemeRedisRetrieverEmpty = new MorphemesRedisRetriever(redisPool, redisKey + "fake", redisKey2 + "fake", minScore = Double.NegativeInfinity)

  def zcardOfRedisKey(key: String) = {
    redisPool.withClient(redis =>
      redis.zcard(key)) match {
      case Some(x: Long) => x.toInt
      case _ => 0
    }
  }

  before {
    redisPool.withClient(redis => redis.flushdb)
    morphemeRedisTracker.storeAllInRedis
    morphemeRedisTracker2.storeAllInRedis
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