//import sbtdocker.DockerPlugin
//import sbtdocker.immutable.Dockerfile

name := "chapter-8"

version := "1.0"

lazy val http4sVersion = "0.14.6" // "0.20.0" //"0.15.0" //"0.14.6"
//lazy val catsVersion: String = "2.0.0"
lazy val scalazVersion: String = "7.3.6"
lazy val scalazConcVersion: String = "7.2.34"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-argonaut" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion, // added by @statisticallyfit

  // Scalaz
  "org.scalaz" %% "scalaz-core" % "7.3.6",
  "org.scalaz" %% "scalaz-concurrent" % "7.2.34",

  // Cats
  /*"org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel"   %% "cats-macros" % catsVersion,
  "org.typelevel"   %% "cats-kernel" % catsVersion,
  "org.typelevel"   %% "cats-laws" % catsVersion,
  "org.typelevel"   %% "cats-free"  % catsVersion,
  "org.typelevel"   %% "cats-testkit" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion,*/
)


mainClass in Compile := Some("com.reactivemachinelearning.ModelServer")
