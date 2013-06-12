package org.beachape.actors

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

    it("should have the timestamp in there") {
      dummy.stringToSetStorableString(testString, fakeUnixTime) should include(fakeUnixTime.toString)
    }

    it("should have the actual string in there") {
      dummy.stringToSetStorableString(testString, fakeUnixTime) should include(testString)
    }

    it("should remove excessive repeats") {
      val repeatsGalor = "asdddddddf123aaaa11f"
      dummy.stringToSetStorableString(repeatsGalor, fakeUnixTime) should include("asddf123aa11f")
    }
  }
}