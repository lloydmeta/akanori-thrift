package com.beachape.support

import java.io.Closeable
import java.io.Reader

import au.com.bytecode.opencsv.CSVReader

class ScalaCSVReader(reader: Reader) extends Iterator[Array[String]] with Closeable {
  require(reader != null)
  private val csv = new CSVReader(reader)
  private var nextRow = csv.readNext
  override def hasNext = nextRow != null
  override def next = try { nextRow } finally { nextRow = csv.readNext }
  override def close { csv.close }
}