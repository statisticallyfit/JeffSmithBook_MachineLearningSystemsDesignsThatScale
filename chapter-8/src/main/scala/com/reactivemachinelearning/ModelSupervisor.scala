package com.reactivemachinelearning

import org.http4s.server.{Server, ServerApp}
import org.http4s.server.blaze._
import org.http4s._
import org.http4s.dsl._


import scalaz.concurrent.Task


// ModelSupervisor Contrasts with ModelSver
object ModelSupervisor extends ServerApp {

  // Redefining traffic-splitting function
  def splitTraffic(data: String): Task[Response] = {
    data.hashCode % 10 match {
      case x if x < 4 => Client.callA(data)
      case x if x < 6 => Client.callB(data)
        // Defiens last 40pct of traffic as being allocated to modelC
      case _ => Client.callC(data)
    }
  }



  val apiService: HttpService = HttpService {

    case GET -> Root / "predict" / inputData => {
      // Defines the behavior of the server
      val response: Response = splitTraffic(inputData).run

      // Pattern matches on the result of calling a service
      response match {
        // Returns successful responses with the model's prediction forming the body of the responses
        case r: Response if r.status == Ok => Response(Ok).withBody(r.bodyAsText)
        // Returns failed responses with the failure messages forming the body of the responses
        case r => Response(BadRequest).withBody(r.bodyAsText)
      }
    }


  }

  override def server(args: List[String]): Task[Server] = {
    BlazeBuilder
      .bindLocal(8080)
      .mountService(apiService, "/api")
      .mountService(Models.modelA, "/models")
      .mountService(Models.modelB, "/models")
      .mountService(Models.modelC, "/models")
      .start
  }

}
