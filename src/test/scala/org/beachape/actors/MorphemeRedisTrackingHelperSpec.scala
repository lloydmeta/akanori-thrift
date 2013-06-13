package org.beachape.actors

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import org.beachape.analyze.Morpheme

import com.redis.RedisClientPool

class DummyKlass(val redisPool: RedisClientPool) extends MorphemesRedisTrackingHelper

class MorphemeRedisTrackingHelperSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfter {

  val redisPool = new RedisClientPool("localhost", 6379, database = 8)
  val listOfMorphemes = Morpheme.stringToMorphemes("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か") ::: Morpheme.stringToMorphemes("米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事") ::: Morpheme.stringToMorphemes("朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")
  val redisKey = RedisKey("test:RedisKeyForMorphemesTracker")
  val dummy = new DummyKlass(redisPool)

  def zcardOfRedisKey = {
    redisPool.withClient(redis =>
      redis.zcard(redisKey.redisKey)) match {
      case Some(x: Long) => x.toInt
      case _ => 0
    }
  }

  def zSetTotalScoreKeyValue = {
    redisPool.withClient(redis =>
      redis.zscore(redisKey.redisKey, dummy.zSetTotalScoreKey)) match {
      case Some(x: Double) => x
      case _ => 0
    }
  }

  def scoreOfMemberInRedisKey(member: String) = {
    redisPool.withClient(redis =>
      redis.zscore(redisKey.redisKey, member)) match {
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
        dummy.storeAllInRedis(listOfMorphemes, redisKey)
        zcardOfRedisKey should be > (0)
      }

      it("should set the zSetTotalScoreKey properly") {
        dummy.storeAllInRedis(listOfMorphemes, redisKey)
        zSetTotalScoreKeyValue should be > (0.0)
      }

      it("should set the scores properly for all morphemes in the morphemes list passed in") {
        dummy.storeAllInRedis(listOfMorphemes, redisKey)
        listOfMorphemes foreach (morpheme =>
          listOfMorphemes.filter(_ == morpheme).length.toDouble should be(scoreOfMemberInRedisKey(morpheme.surface)))
      }
    }

  }

}