package org.beachape.server

import org.apache.thrift.server.TNonblockingServer
import org.apache.thrift.transport.TNonblockingServerSocket

import akka.actor.ActorRef
import trendServer.gen.TrendThriftServer

object TrendServerBuilder {

  def buildServer(socket: Int = 9090, mainOrchestratorRoundRobin: ActorRef): TNonblockingServer = {
    val transport = new TNonblockingServerSocket(socket)
    val processor = new TrendThriftServer.Processor(new TrendServer(mainOrchestratorRoundRobin))
    val args = new TNonblockingServer.Args(transport)
    args.processor(processor)
    new TNonblockingServer(args)
  }

}