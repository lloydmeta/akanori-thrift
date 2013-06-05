package org.beachape.client

import trendServer.gen.TrendThriftServer
import org.apache.thrift.transport.TFramedTransport
import org.apache.thrift.transport.TSocket
import org.apache.thrift.protocol.TBinaryProtocol

object TrendClient {

  def apply(host: String = "localhost", port: Int = 9090) = {
    val socket = new TSocket(host, port)
    val transport = new TFramedTransport(socket)
    val protocol = new TBinaryProtocol(transport)
    transport.open()
    new TrendThriftServer.Client(protocol)
  }

}