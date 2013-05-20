package org.beachape.actors

trait RedisStorageHelper {

  def storedStringsSetKey = "trends:storedStrings"

  def defaultTrendCacheKey = "default:TrendCacheKey"
  def customTrendCacheKey = "default:customTrendCacheKey"
  def customTrendCacheKeyEndingNow = "default:customTrendCacheKeyEndingNow"

  def stringToSetStorableString(stringToStore: String, unixCreateTime: Int) = {
    val uniqueMarker = storedStringUniqueMarker(unixCreateTime)
    f"$uniqueMarker%s$stringToStore%s"
  }

  def storedStringToString(storedString: String) = {
    storedString.replaceFirst(f"$storedStringUniqueMarkerOpener%s.*$storedStringUniqueMarkerCloser%s", "")
  }

  private def storedStringUniqueMarker(unixCreateTime: Int) = f"$storedStringUniqueMarkerOpener%s$unixCreateTime%d$storedStringUniqueMarkerCloser%s"
  private def storedStringUniqueMarkerOpener = "<--createdAt--"
  private def storedStringUniqueMarkerCloser = "--createdAt-->"

}