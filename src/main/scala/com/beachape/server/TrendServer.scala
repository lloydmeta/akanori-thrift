package com.beachape.server

import scala.collection.JavaConversions.seqAsJavaList
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.server.TThreadedSelectorServer
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TNonblockingServerSocket
import com.beachape.actors.FetchTrendsEndingAt
import com.beachape.actors.GetDefaultTrends
import com.beachape.actors.StoreString

import akka.actor.ActorRef
import akka.actor.actorRef2Scala
import akka.pattern.ask
import akka.util.Timeout
import trendServer.gen.TrendResult
import trendServer.gen.TrendThriftServer

import scala.language.postfixOps
import scala.language.implicitConversions
import com.beachape.analyze.Morpheme
import com.typesafe.scalalogging.slf4j.Logging

/**
 * Companion object that holds the factory for TrendServer
 */
object TrendServer {

  /**
   * Returns a TThreadedSelectorserver instance
   *
   * @param mainOrchestrator an ActorRef pointing to a MainOrchestrator actor
   * @param socket to run the Thrift server at (defaults to 9090)
   * @param selectorThreads number of selector threads (network thread) to run (defaults to 16)
   * @param workerThreads number of worker threads to run (defaults to 32)
   */
  def apply(
    mainOrchestrator: ActorRef,
    socket: Int = 9090,
    selectorThreads: Int = 16,
    workerThreads: Int = 32): TThreadedSelectorServer = {
    val transport = new TNonblockingServerSocket(socket)
    val processor = new TrendThriftServer.Processor(new TrendServer(mainOrchestrator))
    val transportFactory = new TFramedTransport.Factory()
    val protocolFactory = new TBinaryProtocol.Factory()

    val args = new TThreadedSelectorServer.Args(transport)
    args.processor(processor)
    args.transportFactory(transportFactory)
    args.protocolFactory(protocolFactory)
    args.selectorThreads(selectorThreads)
    args.workerThreads(workerThreads)

    new TThreadedSelectorServer(args)
  }

}

/**
 * TrendServer implementation of the Thrift server interface.
 *
 * Overrides a bunch of methods as required
 *
 * Should be constructed via the factory method in the companion object
 */
class TrendServer(mainOrchestrator: ActorRef) extends TrendThriftServer.Iface with Logging {
  implicit val timeout = Timeout(600 seconds)

  implicit def toIntegerTrendResultsList(lst: List[TrendResult]) =
    seqAsJavaList(lst.map(i => i: TrendResult))

  override def time: Long = {
    val now = System.currentTimeMillis / 1000
    logger.info("somebody just asked me what time it is: " + now)
    now
  }

  // Just gets from the cached key if it exists...if it doesn't, too bad so sad, Nil
  override def currentTrendsDefault = {
    val listOfReverseSortedTermsAndScoresFuture = ask(mainOrchestrator, GetDefaultTrends)
    val listOfReverseSortedTermsAndScores = Await.result(listOfReverseSortedTermsAndScoresFuture, 600 seconds).asInstanceOf[Option[List[(String, Double)]]]
    for ((term: String, score: Double) <- listOfReverseSortedTermsAndScores.getOrElse(Nil)) yield new TrendResult(term, score)
  }

  override def currentTrends(
    spanInSeconds: Int,
    minOccurrence: Double,
    minLength: Int,
    maxLength: Int,
    top: Int,
    dropBlacklisted: Boolean,
    onlyWhitelisted: Boolean) = {
    (System.currentTimeMillis / 1000).toInt
    val listOfReverseSortedTermsAndScoresFuture = mainOrchestrator ? FetchTrendsEndingAt(
      (System.currentTimeMillis / 1000).toInt,
      spanInSeconds,
      minOccurrence,
      minLength,
      maxLength,
      top,
      dropBlacklisted,
      onlyWhitelisted)
    val listOfReverseSortedTermsAndScores = Await.result(listOfReverseSortedTermsAndScoresFuture, 600 seconds).asInstanceOf[Option[List[(String, Double)]]]
    for ((term: String, score: Double) <- listOfReverseSortedTermsAndScores.getOrElse(Nil)) yield new TrendResult(term, score)
  }

  override def trendsEndingAt(
    unixEndAtTime: Int,
    spanInSeconds: Int,
    minOccurrence: Double,
    minLength: Int,
    maxLength: Int,
    top: Int,
    dropBlacklisted: Boolean,
    onlyWhitelisted: Boolean) = {
    val listOfReverseSortedTermsAndScoresFuture = mainOrchestrator ? FetchTrendsEndingAt(
      unixEndAtTime,
      spanInSeconds,
      minOccurrence,
      minLength,
      maxLength,
      top,
      dropBlacklisted,
      onlyWhitelisted)
    val listOfReverseSortedTermsAndScores = Await.result(listOfReverseSortedTermsAndScoresFuture, 600 seconds).asInstanceOf[Option[List[(String, Double)]]]
    for ((term: String, score: Double) <- listOfReverseSortedTermsAndScores.getOrElse(Nil)) yield new TrendResult(term, score)
  }

  override def storeString(stringToStore: String, userId: String, unixCreatedAtTime: Int, weeksAgoDataToExpire: Int): Unit = {
    mainOrchestrator ! StoreString(stringToStore, userId, unixCreatedAtTime, weeksAgoDataToExpire)
  }

  override def stringToWords(stringToAnalyse: String) = Morpheme.stringToWords(stringToAnalyse)

}