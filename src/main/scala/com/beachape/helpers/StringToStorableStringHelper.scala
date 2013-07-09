package com.beachape.helpers

import com.beachape.support.FloorableToClosestMultipleInt.int2FloorableToClosestMultipleInt

trait StringToStorableStringHelper {

  val createdAtFloorToNearest = 10800

  private val storedStringUniqueMarkerOpener = "<--meta--"
  private val storedStringUniqueMarkerCloser = "--meta-->"
  private val attributesSeparator = "|"
  private val metaKeyValueSeparator = "~"
  private val userIdKey = "userId"
  private val createdAtKey = "createdAt"

  def stringToSetStorableString(stringToStore: String, userId: String, unixCreateTime: Int): String = {
    val stringMetaData = buildMetaData(userId, unixCreateTime)
    val stringWithoutExcessiveRepeats = removeExcessiveRepeatedChars(stringToStore)
    f"$stringMetaData%s$stringWithoutExcessiveRepeats%s"
  }

  def storedStringToString(storedString: String): String = {
    storedString.replaceFirst(f"$storedStringUniqueMarkerOpener%s.*$storedStringUniqueMarkerCloser%s", "")
  }

  private def buildMetaData(userId: String, unixCreateTime: Int) = {
    // Floor the created at to the nearest multiple of createdAtFloorToNearest seconds to prevent
    // users from repeating the same text multiple times and having those counted during trend detection
    val flooredUnixCreateTime = unixCreateTime.floorToClosestMultipleOf(createdAtFloorToNearest)
    val keyAttributeString = f"$userIdKey%s$metaKeyValueSeparator%s$userId%s$attributesSeparator$createdAtKey%s$metaKeyValueSeparator%s$flooredUnixCreateTime%d"
    f"$storedStringUniqueMarkerOpener%s$keyAttributeString%s$storedStringUniqueMarkerCloser%s"
  }

  private def removeExcessiveRepeatedChars(stringToStore: String) = stringToStore.replaceAll("(.)\\1{2,}", "$1$1")
}