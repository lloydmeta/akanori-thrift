package com.beachape.actors

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit

class RedisStringSetToMorphemesActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout {

  val redisPool = new RedisClientPool("localhost", 6379, database = 3)

  val unixTimeStart = UnixTime(10000)
  val unixTimeEnd = UnixTime(20000)
  val unixTimeSpan = UnixTimeSpan(unixTimeStart, unixTimeEnd)

  val listOfStrings = List("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か", "米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事", "朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")

  val redisStringSetToMorphemesActorRef = TestActorRef(new RedisStringSetToMorphemesActor(redisPool))
  val redisStringSetToMorphemesActor = redisStringSetToMorphemesActorRef.underlyingActor

  val storedStringsSetKey = redisStringSetToMorphemesActor.storedStringsSetKey

  before {
    redisPool.withClient(redis => redis.flushdb)
    for (
      unixCreatedAtTime <- (unixTimeStart.time to unixTimeEnd.time).toList;
      storableString <- listOfStrings
    ) {
      redisPool.withClient {
        redis =>
          {
            redis.zadd(storedStringsSetKey, unixCreatedAtTime.toDouble, storableString)
          }
      }
    }
  }

  describe("methods testing") {

    describe("#mapEachPagedListOfTermsInUnixTimeSpan") {

      it("should return a list of the return callback function type") {
        val returnsList = redisStringSetToMorphemesActor.mapEachPagedListOfTermsInUnixTimeSpan(unixTimeSpan) {
          listTerms => true
        }
        returnsList.forall(_ == true) should be(true)
      }

    }

    describe("#dumpListOfStringsToMorphemes") {

      it("should return true") {
        redisStringSetToMorphemesActor.dumpListOfStringsToMorphemes(listOfStrings, RedisKey("testing"), true, true) should be(true)
      }

    }

  }

}

