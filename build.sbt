seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "akanori-thrift"

version := "1.0"

scalaVersion := "2.10.1"

resolvers += "Atilika Open Source repository" at "http://www.atilika.org/nexus/content/repositories/atilika"

resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "net.debasishg" % "redisclient_2.10" % "2.10",
  "org.apache.thrift" % "libthrift" % "0.9.0",
  "com.github.nscala-time" % "nscala-time_2.10" % "0.4.2",
  "com.typesafe.akka" % "akka-testkit_2.10" % "2.1.4",
  "net.sf.opencsv" % "opencsv" % "2.3",
  "org.atilika.kuromoji" % "kuromoji" % "0.7.7",
  "com.typesafe.akka" %% "akka-agent" % "2.1.4",
  "com.typesafe" %% "scalalogging-slf4j" % "1.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.1",
  "ch.qos.logback" % "logback-classic" % "1.0.3"
)

testOptions in Test += Tests.Setup(classLoader =>
  classLoader
    .loadClass("org.slf4j.LoggerFactory")
    .getMethod("getLogger", classLoader.loadClass("java.lang.String"))
    .invoke(null, "ROOT")
)