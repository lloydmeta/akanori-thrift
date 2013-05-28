package org.beachape.support

object RichRange {
  implicit def range2RichRange(r: Range) = RichRange(r)
}

case class RichRange(range: Range) {

  def listOfConsecutivePairsInSteps(step: Int) = {
    val steppedRange = range by step

    def splitIntoConsecutivePairs(xs: List[Int]):List[(Int,Int)] = {
      xs match {
        case x::y::Nil => {
          if (y != range.end) {
            List((x, range.end))
          } else {
            List((x, y))
          }
        }
        case x::y::ys => List((x,y)) ::: splitIntoConsecutivePairs(y::ys)
        case _ => Nil
      }
    }

    splitIntoConsecutivePairs(steppedRange.toList)
  }
}