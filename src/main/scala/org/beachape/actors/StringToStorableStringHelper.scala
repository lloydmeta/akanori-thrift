package org.beachape.actors

trait StringToStorableStringHelper {

  def stringToSetStorableString(stringToStore: String, unixCreateTime: Int) = {
    val uniqueMarker = storedStringUniqueMarker(unixCreateTime)
    val stringWithoutExcessiveRepeats = removeExcessiveRepeatedChars(stringToStore)
    f"$uniqueMarker%s$stringWithoutExcessiveRepeats%s"
  }

  def storedStringToString(storedString: String) = {
    storedString.replaceFirst(f"$storedStringUniqueMarkerOpener%s.*$storedStringUniqueMarkerCloser%s", "")
  }

  private def storedStringUniqueMarker(unixCreateTime: Int) = f"$storedStringUniqueMarkerOpener%s$unixCreateTime%d$storedStringUniqueMarkerCloser%s"
  private def storedStringUniqueMarkerOpener = "<--createdAt--"
  private def storedStringUniqueMarkerCloser = "--createdAt-->"
  private def removeExcessiveRepeatedChars(stringToStore: String) = stringToStore.replaceAll("(.)\\1{2,}", "$1$1")
}