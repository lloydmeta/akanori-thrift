package org.beachape.actors

import org.beachape.analyze.MorphemesRedisRetriever
import org.beachape.testing.Support
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

import akka.actor.ActorSystem
import akka.testkit.DefaultTimeout
import akka.testkit.ImplicitSender
import akka.testkit.TestActorRef
import akka.testkit.TestKit

class MorphemesTrendDetectActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout
  with Support {

  val redisPool = new RedisClientPool("localhost", 6379, database = 2)

  val (oldSet: RedisKeySet, newSet: RedisKeySet) = dumpMorphemesToRedis
  val RedisKeySet(oldExpectedKey: RedisKey, oldObservedKey: RedisKey) = oldSet
  val RedisKeySet(newExpectedKey: RedisKey, newObservedKey: RedisKey) = newSet

  val morphemesTrendDetectActorRef = TestActorRef(new MorphemesTrendDetectActor(redisPool))
  val morphemesTrendDetectActor = morphemesTrendDetectActorRef.underlyingActor

  val oldSetMorphemesRetriever = MorphemesRedisRetriever(redisPool, oldSet.expectedKey.redisKey, oldSet.observedKey.redisKey, 0)
  val newSetMorphemesRetriever = MorphemesRedisRetriever(redisPool, newSet.expectedKey.redisKey, newSet.observedKey.redisKey, 0)

  val oldSetExpectedTotalScore = oldSetMorphemesRetriever.totalExpectedSetMorphemesScore
  val oldSetObservedTotalScore = oldSetMorphemesRetriever.totalObservedSetMorphemesScore

  val newSetExpectedTotalScore = newSetMorphemesRetriever.totalExpectedSetMorphemesScore
  val newSetObservedTotalScore = newSetMorphemesRetriever.totalObservedSetMorphemesScore

  val newObservedSetCard = newSetMorphemesRetriever.observedZCard

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpMorphemesToRedis
  }

  describe("sending messages") {

    describe("sending CalculateAndStoreTrendiness ") {

      val message = CalculateAndStoreTrendiness(
        "lol",
        3.0,
        RedisKey("a key"),
        oldSetMorphemesRetriever,
        newSetMorphemesRetriever,
        oldSetObservedTotalScore,
        oldSetExpectedTotalScore,
        newSetObservedTotalScore,
        newSetExpectedTotalScore)

      it("should return a RedisKeySet") {
        morphemesTrendDetectActorRef ! message
        expectMsg(true)
      }

    }

  }

}