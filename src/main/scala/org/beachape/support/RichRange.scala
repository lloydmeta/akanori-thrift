package org.beachape.support

import scala.annotation.tailrec

object RichRange {
  implicit def range2RichRange(r: Range) = RichRange(r)
}

case class RichRange(range: Range) {

  def listOfConsecutivePairsInSteps(step: Int) = {
    val steppedRange = range by step

    @tailrec def splitIntoConsecutivePairs(xs: List[Int], acc: List[(Int, Int)]): List[(Int, Int)] = {
      xs match {
        case x :: y :: Nil => {
          if (y != range.end) {
            (x, range.end) :: acc
          } else {
            (x, y) :: acc
          }
        }
        case x :: Nil => List((x, range.end)) ::: acc
        case x :: y :: ys => splitIntoConsecutivePairs(y :: ys, (x, y) :: acc)
      }
    }

    splitIntoConsecutivePairs(steppedRange.toList, Nil).reverse
  }
}