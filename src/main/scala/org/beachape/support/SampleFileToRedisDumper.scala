package org.beachape.support

import com.redis._
import org.beachape.actors.RedisStorageHelper
import org.beachape.support.RichRange._

import scala.io.Source

case class SampleFileToRedisDumper(redisPool: RedisClientPool)
  extends RedisStorageHelper {

  def dumpToRedis(filePath: String, unixStartTime: Int, unixEndTime: Int) = {
    val linesPerSecondUseful = linesPerSecondAdjusted(filePath, unixStartTime, unixEndTime)
    val reverseTimeRange = (unixEndTime to unixStartTime by -1)
    val linesPerSecondIterator = Source.fromFile(filePath).getLines().grouped(linesPerSecondUseful)

    for (unixTimeStamp <- reverseTimeRange) {
      if (linesPerSecondIterator.hasNext) {
        val groupOfLines = linesPerSecondIterator.next
        groupOfLines foreach { line =>
          storeLineInRedis(storedStringsSetKey, line, unixTimeStamp)
        }
      }
    }
  }

  def storeLineInRedis(redisKey: String, line: String, unixTimeStamp: Int) = {
    redisPool.withClient {
      redis =>
        {
          redis.zadd(redisKey, unixTimeStamp, line)
        }
    }
  }

  def linesPerSecondAdjusted(filePath: String, unixStartTime: Int, unixEndTime: Int) = {
    if (unixStartTime == unixEndTime) throw new Exception("start time is end time")
    val linesInFile = Source.fromFile(filePath).getLines.size
    val linesPerSecond = linesInFile / (unixEndTime - unixStartTime).abs
    if (linesPerSecond < 1) 1 else linesPerSecond
  }

}