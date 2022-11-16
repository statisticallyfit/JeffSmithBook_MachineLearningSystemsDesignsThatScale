package com.reactivemachinelearning

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import spray.json.RootJsonFormat

import scala.util.Random
import spray.json._
import spray.json.DefaultJsonProtocol
import spray.json.DefaultJsonProtocol.{CharJsonFormat, DoubleJsonFormat, StringJsonFormat, mapFormat}

import scala.concurrent.{ExecutionContextExecutor, Future}


// Case class for Predictions
// Define schema of what your predictions will look like, + how predictions can be serialized as JSON and
// deserialized back into case classes
case class Prediction(id: Long, timestamp: Long, value: Double)

// Uses JSON formatting from SprayJSON
trait Protocols extends DefaultJsonProtocol {
	implicit val ipInfoFormat: RootJsonFormat[Prediction] = jsonFormat3(Prediction.apply)

}

object ExampleModel {

	// Defines a dummy model that operates on features that are structured as maps of char -> double
	def model(features: Map[String, Double]): Prediction = {

		// Creates  a map of feature identifiers to "coeffs"
		val coefficients: Map[String, Int] = ('a' to 'z').map(_.toString).zip(1 to 26).toMap

		// Generates a prediction value by operating on all feature values
		val predictionValue: Double = features.map {
			case (identifier, value) => coefficients.getOrElse(identifier, 0) * value
		}.sum / features.size

		// Instantiates, returning a prediction instance
		Prediction(id = Random.nextLong(), timestamp = System.currentTimeMillis(), value = predictionValue)
	}


	// To convert a feature passed as strings into maps of strings to doubles
	def parseFeatures(features: String): Map[String, Double] = {
		// Converts the feature strings using a parser and convertor from Spray JSON
		// TODO figure out why the implicits are needed (where is the implicit demand from convertTo function?)
		features.parseJson.convertTo[Map[String, Double]]
	}


	// Prediction function - composes a model and feature parser to produce predictions
	def predict(features: String): Prediction = {
		model(parseFeatures(features))
	}
}

// Creates a Service trait containing your formatting functionality
trait Service extends Protocols {

	// Requires an implit actor system for Akka
	implicit val system: ActorSystem
	// Requires an implicit ExecutionContextExecutor for Akka
	implicit def executor: ExecutionContextExecutor
	// Requires an implicit Materializer for Akka
	implicit val materializer: Materializer

	val logger: LoggingAdapter

	//  private def parseFeatures(features: String): Map[Long, Double] = {
	//    features.parseJson.convertTo[Map[Long, Double]]
	//  }

	def predict(features: String): Future[Prediction] = {
		Future(Prediction(123, 456, 0.5))
	}

	// Defining routes of service: ...
	// NOTE: Route == RequestContext => Future[RouteResult]
	val routes: Route = {
		// Logs each request and result to a service
		// NOTE: Book way: "model-service"
		logRequestResult("predictive-service") {
			// Defines the prefix of your route to be predicted
			// NOTE: Book way: pathPrefix("predict")
			pathPrefix("ip") {
				// Defines that this path will receive gets only
				(get & path(Segment)) {
					features:String => // Accepts features as a String of JSON
						// Defines how to complete a request
						complete {
							// Calls a prediction function and turns it into a response
							// NOTE: Book way: ToResponseMarshallable(predict(features))
							// TODO find how to get the implicit
							// NOTE; does this way of calling it call the apply function?
							val result: Future[ToResponseMarshallable] = predict(features)
								.map[ToResponseMarshallable] {
								case prediction: Prediction => prediction
								case _ => BadRequest
							}
							result
						}
				}
			}
		}
	}
}


// NOTE Book: has "ModelService" as title
object PredictiveService extends App with Service {
	override implicit val system = ActorSystem()
	override implicit val executor = system.dispatcher
	override implicit val materializer = ActorMaterializer()

	override val logger = Logging(system, getClass)

	Http().bindAndHandle(routes, "0.0.0.0", 9000)
}