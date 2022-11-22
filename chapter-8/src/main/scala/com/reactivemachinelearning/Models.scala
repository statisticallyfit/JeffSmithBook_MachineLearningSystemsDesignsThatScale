package com.reactivemachinelearning

import org.http4s._
import org.http4s.EntityEncoder
import org.http4s.dsl._
// import org.http4s.argonaut._ // TODO trying jsonEncoderOf[Any] but not working to get that implicit value working (?)

import scalaz.concurrent.Task



import scala.util.Random

object Models extends App {

  // TODO HELP FIX why error here?
  implicit val evEntityEnc: EntityEncoder[Response] = implicitly[EntityEncoder[Response]](EntityEncoder.apply)

  // Source of code = https://hyp.is/SYhIbGg7Ee2vp0tKmxNo6w/gitter.im/http4s/http4s?at=588b57ef4c04e9a44e3a1c3b
  /*import org.http4s.circe._
  implicit val responseEncoder: Encoder[Response] = Encoder.forProduct1("response-temp")(u => "empty")
  implicit val responseDecoder: Decoder[Response] = Decoder.forProduct1("response-temp")(Response.apply)
  implicit val responseEntityEncoder: EntityEncoder[Response] = jsonEncoderOf[Response]
  implicit val responseEntityDecoder: EntityDecoder[Response] = jsonOf[Response]*/

    //jsonEncoderOf[Any]
    //implicitly[EntityEncoder[Any]]

  // TODO where is input data?
  // Defines a model as an HTTP service

  val something = HttpService { case GET -> Root / "a" / data => {val resp = true; Ok(s"something")}}

  val modelA = HttpService {

    // Uses pattern-matching to determine that the request to modelA has been received.
    case GET -> Root / "a" / inputData => {
      // Always responds true for model A
      val response: Boolean = true
      //Status.apply("string")

      // TODO how to fix Ok not taking parameters?  https://hyp.is/5aozAmXHEe2jgQdeg_NsbQ/gitter.im/http4s/http4s?at=5dd6f0e2afacdc4de44f126f
      // TODO proof that Ok("code message") works in version 0.16 = https://hyp.is/O7d47mXxEe2llHMrmwUEpw/http4s.org/v0.16/dsl/
      // Returns an OK status code with a model's prediction
      Ok(s"Model A predicted $response.")
    }
  }

  println(s"modelA = $modelA")
  println(s"running modelA = ${modelA.run}")

  val modelB: Task[Response] = HttpService {
    case GET -> Root / "b" / inputData =>

      // Always return false for modelB
      val response: Boolean = false

      Ok(s"Model B predicted $response.")
  }

  println(s"modelB = $modelB")
  println(s"running modelB = ${modelB.run}")


  val modelC: Task[Response] = HttpService {
    case GET -> Root / "c" / inputData => {

      val workingOk: Boolean = Random.nextBoolean()

      val response: Boolean = true

      if (workingOk) {
        Ok(s"Model C predicted $response.")
      } else {
        BadRequest("Model C failed to predict.")
      }
    }
  }

  println(s"modelC = $modelC")
  println(s"running modelC = ${modelC.run}")

}


