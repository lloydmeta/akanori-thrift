package org.beachape.actors
import akka.actor.Actor
import akka.event.Logging
import com.redis._
import org.beachape.analyze.FileMorphemesToRedis
import scala.concurrent.{Await, Future}
import akka.util.Timeout
import scala.concurrent.duration._


class FileToRedisActor(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) extends Actor {

  import context.dispatcher

  def receive = {
    case FilePathSet(FilePath(expectedFilePath), FilePath(observedFilePath)) => {

      val expectedRedisKeyForPath = redisKeyForPath(expectedFilePath)
      val observedRedisKeyForPath = redisKeyForPath(observedFilePath)

      val expectedFileDumpFuture = Future{
        val expectedFileToMorphemes = FileMorphemesToRedis(expectedFilePath, redisPool, expectedRedisKeyForPath, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
        expectedFileToMorphemes.dumpToRedis
      }
      val observedFileDumpFuture = Future{
        val observedFileToMorphemes = FileMorphemesToRedis(observedFilePath, redisPool, observedRedisKeyForPath, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
        observedFileToMorphemes.dumpToRedis
      }

      Await.result(expectedFileDumpFuture, Timeout(600 seconds).duration)
      Await.result(observedFileDumpFuture, Timeout(600 seconds).duration)

      sender ! RedisKeySet(RedisKey(expectedRedisKeyForPath), RedisKey(observedRedisKeyForPath))
    }

    case _ => println("huh?")
  }

  def redisKeyForPath(path: String) = f"trends:$path%s"
}