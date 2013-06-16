package com.beachape.helpers

trait StringToStorableStringHelper {

  def stringToSetStorableString(stringToStore: String, unixCreateTime: Int): String = {
    val uniqueMarker = storedStringUniqueMarker(unixCreateTime)
    val stringWithoutExcessiveRepeats = removeExcessiveRepeatedChars(stringToStore)
    f"$uniqueMarker%s$stringWithoutExcessiveRepeats%s"
  }

  def storedStringToString(storedString: String): String = {
    storedString.replaceFirst(f"$storedStringUniqueMarkerOpener%s.*$storedStringUniqueMarkerCloser%s", "")
  }

  private def storedStringUniqueMarker(unixCreateTime: Int) = f"$storedStringUniqueMarkerOpener%s$unixCreateTime%d$storedStringUniqueMarkerCloser%s"
  private def storedStringUniqueMarkerOpener = "<--createdAt--"
  private def storedStringUniqueMarkerCloser = "--createdAt-->"
  private def removeExcessiveRepeatedChars(stringToStore: String) = stringToStore.replaceAll("(.)\\1{2,}", "$1$1")
}