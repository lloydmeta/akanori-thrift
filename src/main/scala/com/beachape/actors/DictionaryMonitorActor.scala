package com.beachape.actors

import akka.actor.Actor
import akka.actor.Props
import org.atilika.kuromoji.Tokenizer
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds._
import scala.collection.JavaConversions._
import java.io.File
import com.beachape.analyze.Morpheme
import com.typesafe.scalalogging.slf4j.Logging

/** Companion object housing the factory for Props used to instantiate
  *  [[com.beachape.actors.DictionaryMonitorActor]] */
object DictionaryMonitorActor {

  /**
   * Returns the Props required to spawn an instance of DictionaryMonitorActor
   *
   * @param dictionaryPath a string Path to the custom dictionary file
   */
  def apply(dictionaryPath: String) = Props(new DictionaryMonitorActor(dictionaryPath))
}

/**
 * Actor that monitors a file designated as a dictionary
 * for changes and if there are changes to the file,
 * updates the singleton Morpheme object with a new Tokenizer
 *
 * Should be instantiated via the factory method in
 * the companion object above
 */
class DictionaryMonitorActor(dictionaryPath: String) extends Actor with Logging {

  // Get the directories and file sorted out
  private val dictionaryFile = new File(dictionaryPath)
  private val dictionaryDirString = dictionaryFile.getParent
  private val dictionaryFileNameString = dictionaryFile.getName
  private val dictionaryDir = Paths.get(dictionaryDirString)

  private val watchService = dictionaryDir.getFileSystem.newWatchService
  dictionaryDir.register(watchService, ENTRY_MODIFY)

  beginWatching()

  def receive = {
    case _ => logger.error("This actor does not care about messages.")
  }

  private def beginWatching() {
    while(true) {
      val watchKey = watchService.take()
      for (event <- watchKey.pollEvents()) {
        val changed = event.context.asInstanceOf[Path]
        logger.info(s"Modified: $changed")
        if (changed.endsWith(dictionaryFileNameString)) {
          updateMorphemeTokenizer()
        }
      }
      // reset the key
      val valid = watchKey.reset()
      if (!valid) {
        logger.info("Key has been unregistered")
      }
    }
  }

  private def updateMorphemeTokenizer() {
    logger.info(s"Dictionary file updated $dictionaryFileNameString")
    try {
      val newTokenizer = Tokenizer.builder().userDictionary(dictionaryPath).build()
      Morpheme.tokenizer = newTokenizer
      logger.info("Morpheme Tokenizer updated")
    } catch {
      case e:java.lang.ArrayIndexOutOfBoundsException => logger.error("Formatting seems borked on updated dictionary file, try fixing it (restart not required)")
      case e:Exception => logger.error("Something went wrong when trying to update the Morphemes tokenizer with a new dictionary file, try fixing the file (restart not required)")
    }
  }

}
