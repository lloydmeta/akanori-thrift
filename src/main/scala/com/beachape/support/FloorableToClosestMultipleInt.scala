package com.beachape.support

import scala.language.implicitConversions

object FloorableToClosestMultipleInt {
  implicit def int2FloorableToClosestMultipleInt(int: Int) = FloorableToClosestMultipleInt(int)
}

case class FloorableToClosestMultipleInt(integer: Int) {

  def floorToClosestMultipleOf(toNearest: Int) = {
    (integer / toNearest) * toNearest
  }

}
