package com.reactivemachinelearning


//import org.http4s.argonaut._
import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze._
import org.http4s._
import org.http4s.dsl._

import scalaz.concurrent.Task


// Defines the model-serving service as a ServerApp for a graceful shutdown
object ModelServer extends ServerApp {

  // TODO trying to fix the HttpService error - lacks implicit
  //implicit def responseEncoder: EntityEncoder[Response] = jsonEncoderOf[Response]


  // Function to split traffic based on input
  def splitTraffic(data: String): Task[Response] = {
    // Hashes the input and takes the modulus to determine which model to choose
    data.hashCode % 10 match {
              // Selects model A 40% of the time
      case x if x < 4 => Client.callA(data)
      case _ => Client.callB(data)
    }
  }


  // Defines another HttpService to be the primary API endpoint for external usage
  val apiService: HttpService = HttpService {

    case GET -> Root / "predict" / inputData =>

      // Passes input data to a traffic-splitting function, and immediately invokes it
      // NOTE: calls .run on a Task[Response]
      val response: Response = splitTraffic(inputData).run

      // Passes through response with an OK status
      Ok(response.toString())

  }

  // Defines the behavior of the server
  override def server(args: List[String]): Task[Server] = {

    // Uses the built-in backend from Blaze to build the server
    BlazeBuilder
      .bindLocal(8080) // Binds to port 8080 on a local machine
      .mountService(apiService, "/api") // Mounts an API service to the path at /api
      .mountService(Models.modelA, "/models") // Attaches a service for modelA to the server at /models
      .mountService(Models.modelB, "/models") // Attaches a service for modelB to the server at /models
      .start
  }

}