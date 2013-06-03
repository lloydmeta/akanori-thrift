package org.beachape.support

import com.redis._
import java.io._
import org.beachape.actors.RedisStorageHelper
import org.beachape.support.RichRange._
import com.github.nscala_time.time.Imports._

import scala.io.Source

case class SampleFileToRedisDumper(redisPool: RedisClientPool)
  extends RedisStorageHelper {

  def dumpToRedis(filePath: String, unixStartTime: Int, unixEndTime: Int) = {
    if (filePath.contains(".csv")) {
      dumpFromCSV(filePath)
    } else {
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

  def dumpFromCSV(filePath: String) = {
    val reader = new ScalaCSVReader(new FileReader(filePath))
    val currentTime = System.currentTimeMillis / 1000

    val firstEntry = reader.next
    val newestDateString = firstEntry(1) //assume second element is a string
    val newestDateUnixTimestamp = (newestDateString.toDateTime.millis / 1000).toInt
    storeLineInRedis(storedStringsSetKey, firstEntry(0), newestDateUnixTimestamp)

    reader foreach { row =>
      if (row.length == 2)
        try {
          val string = row(0)
          val dateString = row(1)
          val dateUnix = dateString.toDateTime.millis / 1000
          val dateUnixAdjusted = (currentTime - (newestDateUnixTimestamp - dateUnix)).toInt
          storeLineInRedis(storedStringsSetKey, string, dateUnixAdjusted)
          val dateAdjusted = (dateUnixAdjusted * 1000L).toDateTime
          println(s"$string inserted at $dateAdjusted")
        } catch {
          case _:Throwable =>
        }
    }
    reader.close
  }

}