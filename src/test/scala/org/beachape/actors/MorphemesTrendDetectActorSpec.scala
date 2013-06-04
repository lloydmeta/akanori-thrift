package org.beachape.actors

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

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpMorphemesToRedis
  }

  describe("sending messages") {

    describe("sending List('detectTrends, (oldSet: RedisKeySet, newSet: RedisKeySet, minOccurrence: Double)) ") {

      it("should return a RedisKeySet") {
        morphemesTrendDetectActorRef ! List('detectTrends, (oldSet, newSet, 0.0))
        expectMsgType[RedisKeySet]
      }

    }

  }

}