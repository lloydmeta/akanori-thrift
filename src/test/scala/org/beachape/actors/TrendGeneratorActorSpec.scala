package org.beachape.actors

import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{ TestActorRef, TestKit, ImplicitSender, DefaultTimeout }
import scala.concurrent.duration._
import scala.concurrent.{ Future, Await }
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import org.beachape.testing.Support
import scala.util.{ Try, Success, Failure }

class TrendGeneratorActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout
  with Support{

  val redisPool = new RedisClientPool("localhost", 6379, database = 5)

  val (oldSet: RedisKeySet, newSet: RedisKeySet) = dumpMorphemesToRedis
  val RedisKeySet(oldExpectedKey: RedisKey, oldObservedKey: RedisKey) = oldSet
  val RedisKeySet(newExpectedKey: RedisKey, newObservedKey: RedisKey) = newSet

  val trendGeneratorActorRef = TestActorRef(new TrendGeneratorActor(redisPool, true, true))
  val trendGeneratorActor = trendGeneratorActorRef.underlyingActor

  before {
    redisPool.withClient(redis => redis.flushdb)
    dumpMorphemesToRedis
  }
}