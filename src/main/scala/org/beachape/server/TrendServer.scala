package org.beachape.server

import scala.collection.JavaConversions.seqAsJavaList
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import trendServer.gen.TrendResult
import trendServer.gen.TrendThriftServer

class TrendServer(mainOrchestrator: ActorRef) extends TrendThriftServer.Iface {
  implicit val timeout = Timeout(600 seconds)

  implicit def toIntegerTrendResultsList(lst: List[TrendResult]) =
    seqAsJavaList(lst.map(i => i: TrendResult))

  override def time: Long = {
    val now = System.currentTimeMillis / 1000
    println("somebody just asked me what time it is: " + now)
    now
  }

  override def currentTrendsDefault = {
    val listOfReverseSortedTermsAndScoresFuture = ask(mainOrchestrator, 'getTrendsDefault)
    val listOfReverseSortedTermsAndScores = Await.result(listOfReverseSortedTermsAndScoresFuture, 600 seconds).asInstanceOf[List[(String, Double)]]
    val listOfTrendResults = for (result <- listOfReverseSortedTermsAndScores) yield {
      result match {
        case (term: String, score: Double) => new TrendResult(term, score)
        case _ => new TrendResult("fail", -55378008)
      }
    }
    listOfTrendResults
  }

  override def currentTrends(spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) = {
    (System.currentTimeMillis / 1000).toInt
    val listOfReverseSortedTermsAndScoresFuture = ask(mainOrchestrator, List('getTrendsEndingAt, ((System.currentTimeMillis / 1000).toInt, spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)))
    val listOfReverseSortedTermsAndScores = Await.result(listOfReverseSortedTermsAndScoresFuture, 600 seconds).asInstanceOf[List[(String, Double)]]
    val listOfTrendResults = for (result <- listOfReverseSortedTermsAndScores) yield {
      result match {
        case (term: String, score: Double) => new TrendResult(term, score)
        case _ => new TrendResult("fail", -55378008)
      }
    }
    listOfTrendResults
  }

  override def trendsEndingAt(unixEndAtTime: Int, spanInSeconds: Int, minOccurrence: Double, minLength: Int, maxLength: Int, top: Int, dropBlacklisted: Boolean, onlyWhitelisted: Boolean) = {
    val listOfReverseSortedTermsAndScoresFuture = ask(mainOrchestrator, List('getTrendsEndingAt, (unixEndAtTime, spanInSeconds, minOccurrence, minLength, maxLength, top, dropBlacklisted, onlyWhitelisted)))
    val listOfReverseSortedTermsAndScores = Await.result(listOfReverseSortedTermsAndScoresFuture, 600 seconds).asInstanceOf[List[(String, Double)]]
    val listOfTrendResults = for (result <- listOfReverseSortedTermsAndScores) yield {
      result match {
        case (term: String, score: Double) => new TrendResult(term, score)
        case _ => new TrendResult("fail", -55378008)
      }
    }
    listOfTrendResults
  }

  override def storeString(stringToStore: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int): Unit = {
    mainOrchestrator ! List('storeString, (stringToStore, unixCreatedAtTime, weeksAgoDataToExpire))
  }

}