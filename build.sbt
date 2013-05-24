seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "akanori"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.chasen.mecab" % "mecab-java" % "0.993",
  "net.debasishg" % "redisclient_2.10" % "2.10",
  "org.apache.thrift" % "libthrift" % "0.9.0"
)