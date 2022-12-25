name := "chapter-9"

version := "1.0"
    


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

)

// Sets the main class to be executed when the archive is run. 
mainClass in Compile := Some("com.reactivemachinelearning.ModelServer")
