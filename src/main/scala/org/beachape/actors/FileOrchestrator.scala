package org.beachape.actors

import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging
import com.redis._
import org.beachape.analyze.FileMorphemesToRedis

class FileToRedisActor(redisPool: RedisClientPool, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) extends Actor {
  def receive = {
    case filePath: String => {
      val redisKeyForPath = f"trends:$filePath%s"
      println(f"Dumping file at: $filePath%s")
      val fileToMorphemes = FileMorphemesToRedis(filePath, redisPool, redisKeyForPath, dropBlacklisted = dropBlacklisted, onlyWhitelisted = onlyWhitelisted)
      fileToMorphemes.dumpToRedis
      println(f"Done ! $redisKeyForPath%s")
    }
    case _ => println("huh?")
  }
}