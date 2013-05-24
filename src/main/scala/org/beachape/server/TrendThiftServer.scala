package org.beachape.server

import trendServer.gen._

class ThriftServer extends TrendThriftServer.Iface {
  override def time: Long = {
    val now = System.currentTimeMillis / 1000
    println("somebody just asked me what time it is: " + now)
    now
  }
}