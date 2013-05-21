seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

name := "mecab_test"

version := "1.0"

scalaVersion := "2.10.0"

libraryDependencies ++= Seq(
  "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test",
  "org.scala-tools.testing" % "specs_2.10" % "1.6.9" % "test",
  "org.chasen.mecab" % "mecab-java" % "0.993",
  "net.debasishg" % "redisclient_2.10" % "2.10"
)