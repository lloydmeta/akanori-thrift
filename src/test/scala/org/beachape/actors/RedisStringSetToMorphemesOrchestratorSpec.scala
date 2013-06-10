package org.beachape.actors

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit

class RedisStringSetToMorphemesOrchestratorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout {

  val redisPool = new RedisClientPool("localhost", 6379, database = 4)

  val unixTimeStart = UnixTime(1369821708)
  val unixTimeEnd = UnixTime(1369908108)
  val unixTimeSpan = UnixTimeSpan(unixTimeStart, unixTimeEnd)

  val listOfStrings = List("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か", "米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事", "朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")

  val redisStringSetToMorphemesOrchestratorRef = TestActorRef(new RedisStringSetToMorphemesOrchestrator(redisPool))
  val redisStringSetToMorphemesOrchestrator = redisStringSetToMorphemesOrchestratorRef.underlyingActor

  val storedStringsSetKey = redisStringSetToMorphemesOrchestrator.storedStringsSetKey

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

  describe("sending a GenerateMorphemesFor(unixEndAtTime: Int, spanInSeconds: Int, dropBlacklisted: Boolean, onlyWhitelisted: Boolean))") {
    it("should return true") {
      redisStringSetToMorphemesOrchestratorRef ! GenerateMorphemesFor(unixTimeEnd.time, unixTimeEnd.time - 10800, false, false)
      expectMsgType[RedisSetPair]
    }

  }

}

