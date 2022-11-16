//import sbtdocker.DockerPlugin
//import sbtdocker.immutable.Dockerfile

name := "chapter-8"

version := "1.0"

lazy val http4sVersion = "0.14.6"
lazy val catsVersion: String = "2.0.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-argonaut" % http4sVersion,

  // Cats
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel"   %% "cats-macros" % catsVersion,
  "org.typelevel"   %% "cats-kernel" % catsVersion,
  "org.typelevel"   %% "cats-laws" % catsVersion,
  "org.typelevel"   %% "cats-free"  % catsVersion,
  "org.typelevel"   %% "cats-testkit" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsVersion,
)


mainClass in Compile := Some("com.reactivemachinelearning.ModelServer")
