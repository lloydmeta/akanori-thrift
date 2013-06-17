package com.beachape.actors;

import scala.util.Success

import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit

class MorphemesAnalyzerActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout {

  val redisPool = new RedisClientPool("localhost", 6379, database = 1)
  val listOfStrings = List("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か", "米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事", "朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")
  val redisKey = "test:key"
  val morphemesAnalyzerActor = TestActorRef(new MorphemesAnalyzerActor(redisPool))

  before {
    redisPool.withClient(redis => redis.flushdb)
  }

  def dumpListAndRun(verifier: () => Unit) = {
    val listOfFutures = for (string <- listOfStrings) yield {
      (morphemesAnalyzerActor ? AnalyseAndStoreInRedisKey(string, RedisKey(redisKey), true, true)).mapTo[Boolean]
    }

    val listOfFutureValues = listOfFutures map (x => x.value.get)
    if (listOfFutureValues.forall(x => x == Success(true))) {
      verifier()
    } else {
      false should be(true)
    }
  }

  describe("sending a request to dumpMorphemesToRedis") {

    it("should return true") {
      morphemesAnalyzerActor ! AnalyseAndStoreInRedisKey(listOfStrings.head, RedisKey(redisKey), true, true)
      expectMsg(true)
    }

    describe("checking on the redis key") {

      it("should create a key so that it exists") {
        val verifyKeyExists = { () =>
          val keyExists = redisPool.withClient { redis => redis.exists(redisKey) }
          keyExists should be(true)
        }
        dumpListAndRun(verifyKeyExists)
      }

      it("should have a zCard above zero") {
        val verifyzCardMoreThanZero = { () =>
          redisPool.withClient { redis =>
            redis.zcard(redisKey) match {
              case Some(card: Long) => card.toInt should be > (0)
              case _ => false should be(true)
            }
          }
        }
        dumpListAndRun(verifyzCardMoreThanZero)
      }

    }

  }

}