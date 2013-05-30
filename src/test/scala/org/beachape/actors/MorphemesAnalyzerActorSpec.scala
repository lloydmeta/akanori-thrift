import org.beachape.actors.{MorphemesAnalyzerActor, RedisKey}
import com.redis._
import org.scalatest.FunSpec
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.ShouldMatchers
import akka.testkit.{TestActorRef, TestKit, ImplicitSender, DefaultTimeout}
import scala.concurrent.duration._
import scala.concurrent.Await
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.util.Timeout
import scala.util.{Try, Success, Failure}

class MorphemesAnalyzerActorSpec extends TestKit(ActorSystem("akkaTest"))
  with FunSpec
  with ShouldMatchers
  with BeforeAndAfter
  with ImplicitSender
  with DefaultTimeout{

  val redisPool = new RedisClientPool("localhost", 6379, database = 3)
  val listOfStrings = List("笹子トンネル、設計時に風圧見落とし　天井崩落の一因か", "米の戦闘機Ｆ１５、沖縄の東海上に墜落　パイロット無事", "朝井リョウ、アイドル小説構想中　「夢と卒業」テーマ")
  val redisKey = "test:key"
  val morphemesAnalyzerActor = TestActorRef(new MorphemesAnalyzerActor(redisPool))

  before {
    redisPool.withClient(redis => redis.flushdb)
  }

  describe("sending a request to dumpMorphemesToRedis") {
    it("should return true") {
      morphemesAnalyzerActor ! List('dumpMorphemesToRedis, RedisKey(redisKey), listOfStrings.head, true, true)
      expectMsg(true)
    }

  }

}