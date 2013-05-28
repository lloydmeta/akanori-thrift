import org.beachape.analyze.{ MorphemesRedisTracker, Morpheme, RedisHelper }
import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import org.beachape.analyze.MorphemesRedisTracker

class MorphemeRedisTrackerSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter {

  val redisPool = new RedisClientPool("localhost", 6379, database = 5)
  val listOfMorphemes = Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か") ::: Morpheme.stringToMorphemes("米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事") ::: Morpheme.stringToMorphemes("朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")
  val redisKey = "test:RedisKeyForMorphemesTracker"
  val morphemeRedisTracker = new MorphemesRedisTracker(listOfMorphemes, redisPool, redisKey)

  def zcardOfRedisKey = {
    redisPool.withClient(redis =>
      redis.zcard(redisKey)) match {
      case Some(x: Long) => x.toInt
      case _ => 0
    }
  }

  def zSetTotalScoreKeyValue = {
    redisPool.withClient(redis =>
      redis.zscore(redisKey, morphemeRedisTracker.zSetTotalScoreKey)) match {
      case Some(x: Double) => x
      case _ => 0
    }
  }

  def scoreOfMemberInRedisKey(member: String) = {
    redisPool.withClient(redis =>
      redis.zscore(redisKey, member)) match {
      case Some(x: Double) => x
      case _ => 0
    }
  }

  before {
    redisPool.withClient(redis => redis.flushdb)
  }

  describe("#storeAllInRedis") {

    describe("before running") {
      it("should have zero z-cardinality") {
        zcardOfRedisKey should be(0)
      }

      it("should have 0 zSetTotalScoreKeyValue") {
        zSetTotalScoreKeyValue should be(0)
      }
    }

    describe("after running") {

      it("should fill the zSet with morphemes") {
        morphemeRedisTracker.storeAllInRedis
        zcardOfRedisKey should be > (0)
      }

      it("should set the zSetTotalScoreKey properly") {
        morphemeRedisTracker.storeAllInRedis
        zSetTotalScoreKeyValue should be > (0.0)
      }

      it("should set the scores properly for all morphemes in the morphemes list passed in") {
        morphemeRedisTracker.storeAllInRedis
        listOfMorphemes foreach (morpheme =>
          listOfMorphemes.filter(_ == morpheme).length.toDouble should be(scoreOfMemberInRedisKey(morpheme.surface)))
      }
    }

  }

}