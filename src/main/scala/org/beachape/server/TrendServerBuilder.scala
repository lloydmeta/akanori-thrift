package org.beachape.server

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.server.TThreadedSelectorServer
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TNonblockingServerSocket

import akka.actor.ActorRef
import trendServer.gen.TrendThriftServer

object TrendServerBuilder {

  def buildServer(
    socket: Int = 9090,
    mainOrchestratorRoundRobin: ActorRef,
    selectorThreads: Int = 16,
    workerThreads: Int = 32): TThreadedSelectorServer = {
    val transport = new TNonblockingServerSocket(socket)
    val processor = new TrendThriftServer.Processor(new TrendServer(mainOrchestratorRoundRobin))
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