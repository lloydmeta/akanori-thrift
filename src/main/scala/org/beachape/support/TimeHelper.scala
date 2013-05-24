package org.beachape.support

import scala.math.floor
import com.github.nscala_time.time.Imports._

trait TimeHelper {

  def time_floor(milliseconds: Long = 5.minutes.millis): DateTime = {
    new DateTime(floor(DateTime.now.millis.toDouble / milliseconds).toLong * milliseconds)
  }
}