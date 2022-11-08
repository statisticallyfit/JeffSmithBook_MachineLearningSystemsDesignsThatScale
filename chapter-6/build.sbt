name := "chapter-6"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq("com.github.nscala-time" %% "nscala-time" % "2.8.0",
  "org.apache.spark" %% "spark-core" % "2.2.0",
  "org.apache.spark" %% "spark-mllib" % "2.2.0",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test")