package com.beachape.support

import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers
import com.beachape.support.FloorableToClosestMultipleInt.int2FloorableToClosestMultipleInt

class FloorableToClosestMultipleIntSpec extends FunSpec
with ShouldMatchers {

  describe("#floorToClosestMultipleOf") {

    it("should floor to the nearest multiple of a number") {
      14.floorToClosestMultipleOf(5) should be(10)
      15.floorToClosestMultipleOf(5) should be(15)
      10799.floorToClosestMultipleOf(3600) should be(7200)
      10800.floorToClosestMultipleOf(3600) should be(10800)
    }

  }
}
