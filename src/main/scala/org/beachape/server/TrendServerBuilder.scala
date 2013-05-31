package org.beachape.server

import trendServer.gen.TrendThriftServer
import org.apache.thrift._
import org.apache.thrift.protocol._
import org.apache.thrift.server._
import org.apache.thrift.transport._
import akka.actor.ActorRef

object TrendServerBuilder {

  def buildServer(socket: Int = 9090, mainOrchestratorRoundRobin: ActorRef): TNonblockingServer = {
    val transport = new TNonblockingServerSocket(socket)
    val processor = new TrendThriftServer.Processor(new TrendServer(mainOrchestratorRoundRobin))
    val args = new TNonblockingServer.Args(transport)
    args.processor(processor)
    new TNonblockingServer(args)
  }

}