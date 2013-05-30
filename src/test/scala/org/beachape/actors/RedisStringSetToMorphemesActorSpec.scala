import org.beachape.actors.{ MorphemesAnalyzerActor, RedisStringSetToMorphemesActor, RedisKey, UnixTime, UnixTimeSpan }
import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{ TestActorRef, TestKit, ImplicitSender, DefaultTimeout }
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.util.{ Try, Success, Failure }

class RedisStringSetToMorphemesActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout {

  val redisPool = new RedisClientPool("localhost", 6379, database = 1)

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

    describe("#listOfUnixTimeSpanInSteps") {

      it("should yield a list of UnixTimeSpans with the first element having a #start same as the original span thrown in") {
        redisStringSetToMorphemesActor.listOfUnixTimeSpanInSteps(unixTimeSpan).head.start should be(unixTimeSpan.start)
      }

      it("should yield a list of UnixTimeSpans with the last element having an #.end same as the original span thrown in") {
        redisStringSetToMorphemesActor.listOfUnixTimeSpanInSteps(unixTimeSpan).last.end should be(unixTimeSpan.end)
      }

      describe("retrieving strings") {

        it("should allow me to get the same strings as if I had looped over the original span") {
          val retrievedStringsViaStep = redisStringSetToMorphemesActor.listOfUnixTimeSpanInSteps(unixTimeSpan) flatMap { steppedUnixTimeSpan =>
            redisStringSetToMorphemesActor.listOfTermsInRedisStoredSetBetweenUnixTimeSpan(steppedUnixTimeSpan)
          }

          retrievedStringsViaStep should be(redisStringSetToMorphemesActor.listOfTermsInRedisStoredSetBetweenUnixTimeSpan(unixTimeSpan))
        }
      }
    }

  }

}

