import java.io.FileWriter

import org.beachape.support.SampleFileToRedisDumper
import org.scalatest.BeforeAndAfter
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

import com.redis.RedisClientPool

class SampleFileToRedisDumperSpec extends FunSpec
  with BeforeAndAfter
  with ShouldMatchers {

  val redisPool = new RedisClientPool("localhost", 6379, database = 9)
  val dumper = SampleFileToRedisDumper(redisPool)

  val temp1 = java.io.File.createTempFile("temp1", "first")
  val temp2 = java.io.File.createTempFile("temp2", "second")
  val temp1Path = temp1.getAbsolutePath()
  val temp2Path = temp2.getAbsolutePath()
  val temp1Writer = new FileWriter(temp1Path, true)
  val temp2Writer = new FileWriter(temp2Path, true)

  val data1 = List("Some", "strings", "in", "a", "file")
  val data2 = List("Couple", "strings", "Some", "strings", "in", "a", "file", "Some", "strings", "in", "a", "file")

  data1.foreach { x =>
    temp1Writer.write(x + sys.props("line.separator"))
  }
  temp1Writer.close

  data2.foreach { x =>
    temp2Writer.write(x + sys.props("line.separator"))
  }
  temp2Writer.close

  before {
    redisPool.withClient(redis => redis.flushdb)
  }

  describe("#linesPerSecondAdjusted") {

    it("should throw an exception if start and end times are the same") {
      intercept[Exception] {
        dumper.linesPerSecondAdjusted(temp1Path, 1, 1)
      }
    }

    it("should give me the proper number of lines based on the file and the unix start and end times") {
      dumper.linesPerSecondAdjusted(temp1Path, 1, 3) should be(2)
      dumper.linesPerSecondAdjusted(temp1Path, 1, 10) should be(1)
      dumper.linesPerSecondAdjusted(temp2Path, 3, 10) should be(1)
      dumper.linesPerSecondAdjusted(temp2Path, 6, 10) should be(3)
      dumper.linesPerSecondAdjusted(temp2Path, 6, 9) should be(4)
    }

  }

  describe("#storeLineInRedis") {

    it("should store a string properly with the score") {
      dumper.storeLineInRedis("test", "hello", 1234)
      redisPool.withClient {
        redis =>
          {
            redis.zscore("test", dumper.stringToSetStorableString("hello", 1234)) match {
              case Some(x: Double) => x should be(1234)
              case None => true should be(false) // just fail
            }
          }
      }
    }

  }

}
