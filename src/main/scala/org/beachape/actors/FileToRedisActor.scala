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

    case FilePath(filePath) => {
      sender ! RedisKey(dumpFileToRedis(filePath))
    }

    case _ => println("huh?")
  }

  def redisKeyForPath(path: String) = f"trends:$path%s"

  def dumpFileToRedis(filePath: String) = {
    val redisKey = redisKeyForPath(filePath)
    val fileToMorphemes = FileMorphemesToRedis(filePath, redisPool, redisKey, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
    fileToMorphemes.dumpToRedis
    redisKey
  }
}