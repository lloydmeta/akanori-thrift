package org.beachape.support

import java.io.FileReader

import scala.io.Source

import org.beachape.helpers.RedisStorageHelper

import com.github.nscala_time.time.Imports.RichLong
import com.github.nscala_time.time.Imports.RichReadableInstant
import com.github.nscala_time.time.Imports.RichString
import com.redis.RedisClientPool

case class SampleFileToRedisDumper(redisPool: RedisClientPool)
  extends RedisStorageHelper {

  def dumpToRedis(filePath: String, unixStartTime: Int, unixEndTime: Int) {
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

  def storeLineInRedis(redisKey: String, line: String, unixTimeStamp: Int) {
    redisPool.withClient {
      redis =>
        {
          redis.zadd(redisKey, unixTimeStamp, stringToSetStorableString(line, unixTimeStamp))
        }
    }
  }

  def linesPerSecondAdjusted(filePath: String, unixStartTime: Int, unixEndTime: Int): Int = {
    if (unixStartTime == unixEndTime) throw new Exception("start time is end time")
    val linesInFile = Source.fromFile(filePath).getLines.size
    val linesPerSecond = linesInFile / (unixEndTime - unixStartTime).abs
    if (linesPerSecond < 1) 1 else linesPerSecond
  }

  def dumpFromCSV(filePath: String) {
    val reader = new ScalaCSVReader(new FileReader(filePath))
    val currentTime = System.currentTimeMillis / 1000

    reader foreach { row =>
      if (row.length == 2)
        try {
          val string = row(0)
          val dateString = row(1)
          val dateUnix = (dateString.toDateTime.millis / 1000).toInt
          storeLineInRedis(storedStringsSetKey, string, dateUnix)
          val dateTime = (dateUnix * 1000L).toDateTime
          println(s"$string inserted at $dateTime")
        } catch {
          case _: Throwable =>
        }
    }
    reader.close
  }

}