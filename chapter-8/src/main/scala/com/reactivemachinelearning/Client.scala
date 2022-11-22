package com.reactivemachinelearning

import org.http4s.{EntityDecoder, Response, Uri}
import org.http4s.client.Client
import org.http4s.client.blaze.PooledHttp1Client

import scala.concurrent.duration._
import scalaz.concurrent.Task

// Creates object to contain client helpers
object Client {

  // Starts HTTP client to call modeling services
  val client: Client = PooledHttp1Client()

  // Factors out the common steps of calling models and puts these steps in a helper function
  private def call_taskString(model: String, input: String): Task[String] = {
    val target: Uri = Uri.fromString(s"http://localhost:8080/models/$model/$input").toOption.get

    // Creates a Task to define a request
    // TODO compare meaning between `resultExpect` and `resultRetry`:
    // NOTE: this is the old way from book, before refactor to return Task[Response]
    val resultExpect: Task[String] = client.expect[String](target)

    // TODO trying to create the author's 'expect' way but with 'Response' as argument type because that is required
    //  by this function's return type HELP what implicit to use below?
    /*val imp = implicitly[EntityDecoder[Response]]
    val resultExpectResponse: Task[Response] = client.expect[Response](target)*/

    //val resultRetry: Task[Response] = client(target).retry(Seq(1 second), { _ => true })

    //resultRetry
    resultExpect
  }

  // NOTE: refactored call() to return Task[Response]
  private def call_applyMethod(model: String, input: String): Task[Response] = {
    val target: Uri = Uri.fromString(s"http://localhost:8080/models/$model/$input").toOption.get

    client(target)

    // NOTE meaning: client  = Client.apply => toHttpService.run(...)
    // where run() is from Kleisli
    // return type: Task[Response]

    // The Client.apply function:
    /*def apply(uri: Uri): Task[Response] =
      toHttpService.run(Request(Method.GET, uri))*/
  }


  private def call_retryMethod(model: String, input: String): Task[Response] = {
    val target: Uri = Uri.fromString(s"http://localhost:8080/models/$model/$input").toOption.get

    // Retries after 1 second for all failures
    client(target).retry(Seq(1 second), {_ => true})
  }



  // Creates a function to call modelA
  def callA(input: String): Task[Response] = call_applyMethod("a", input)

  // Creates a function to call modelB
  def callB(input: String): Task[Response] = call_applyMethod("b", input)

  // Creates a function to call modelC
  def callC(input: String): Task[Response] = call_applyMethod("c", input)

}
