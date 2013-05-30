import org.beachape.actors.{ RedisStringSetToMorphemesOrchestrator, RedisKey, RedisKeySet, UnixTime, UnixTimeSpan }
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

class RedisStringSetToMorphemesOrchestratorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout {

  val redisPool = new RedisClientPool("localhost", 6379, database = 2)

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

  describe("sending a request to List('generateMorphemesFor, (unixEndAtTime: Int, spanInSeconds: Int, dropBlacklisted: Boolean, onlyWhitelisted: Boolean))") {
    it("should return true") {
      redisStringSetToMorphemesOrchestratorRef ! List('generateMorphemesFor, (unixTimeEnd.time, unixTimeEnd.time - 10800, false, false))
      expectMsgType[List[RedisKeySet]]
    }

  }

}

