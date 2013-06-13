package org.beachape.client

import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TSocket

import trendServer.gen.TrendThriftServer

/**
 * Factory for a Scala client of TrendThriftServer
 */
object TrendClient {

  /**
   * Instantiates an instance of a TrendThriftServer
   * client
   * @param host where the Thrift server is located (optional, defaults to "localhost")
   * @param port that the Thrift server is taking requests on (optional, defaults to 9090)
   */
  def apply(host: String = "localhost", port: Int = 9090) = {
    val socket = new TSocket(host, port)
    val transport = new TFramedTransport(socket)
    val protocol = new TBinaryProtocol(transport)
    transport.open()
    new TrendThriftServer.Client(protocol)
  }

}