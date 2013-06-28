seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "akanori-thrift"

version := "1.0"

scalaVersion := "2.10.1"

resolvers += "Atilika Open Source repository" at "http://www.atilika.org/nexus/content/repositories/atilika"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.chasen.mecab" % "mecab-java" % "0.993",
  "net.debasishg" % "redisclient_2.10" % "2.10",
  "org.apache.thrift" % "libthrift" % "0.9.0",
  "com.github.nscala-time" % "nscala-time_2.10" % "0.4.2",
  "com.typesafe.akka" % "akka-testkit_2.10" % "2.1.4",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.atilika.kuromoji" % "kuromoji" % "0.7.7"
)