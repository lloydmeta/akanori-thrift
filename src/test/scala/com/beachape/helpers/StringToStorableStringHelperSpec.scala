package com.beachape.helpers

import com.beachape.support.FloorableToClosestMultipleInt.int2FloorableToClosestMultipleInt

import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.FunSpec
import org.scalatest.matchers.ShouldMatchers

class DummyClass extends StringToStorableStringHelper

class RedisStorageHelperSpec extends FunSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  val dummy = new DummyClass

  describe("#stringToSetStorableString's return") {

    val testString = "hellothere"
    val fakeUnixTime = 12345678

    it("should have the timestamp, floored to nearest LCM of createdAtFloorToNearest, in there") {
      dummy.stringToSetStorableString(testString, "system", fakeUnixTime) should include(fakeUnixTime.floorToClosestMultipleOf(dummy.createdAtFloorToNearest).toString)
    }

    it("should have the userId, in there") {
      dummy.stringToSetStorableString(testString, "system", fakeUnixTime) should include("system")
    }

    it("should have the actual string in there") {
      dummy.stringToSetStorableString(testString, "system", fakeUnixTime) should include(testString)
    }

    it("should remove excessive repeats") {
      val repeatsGalor = "asdddddddf123aaaa11f"
      dummy.stringToSetStorableString(repeatsGalor, "system", fakeUnixTime) should include("asddf123aa11f")
    }
  }
}