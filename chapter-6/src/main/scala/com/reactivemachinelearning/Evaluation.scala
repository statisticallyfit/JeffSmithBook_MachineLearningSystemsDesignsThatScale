package com.reactivemachinelearning

import com.github.nscala_time.time.Imports._
import org.apache.spark.ml.classification.{BinaryLogisticRegressionSummary, LogisticRegression, LogisticRegressionModel, LogisticRegressionTrainingSummary}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.max

object Evaluation extends App {


	val session: SparkSession = SparkSession.builder().master("local[1]").appName("Fraud Model").getOrCreate()
	import session.implicits._

	val data: DataFrame = session.read.format("libsvm").load("src/main/resources/sample_libsvm_data.txt")
	println("data: ")
	data.show(false)

	val Array(trainingData, testingData) = data.randomSplit(Array(0.8, 0.2))

	val learningAlgo: LogisticRegression = new LogisticRegression()
	learningAlgo.params
	learningAlgo.params.foreach(println(_))
	learningAlgo.getThresholds
	// TODO these don't actually give any info!??

	val model: LogisticRegressionModel = learningAlgo.fit(trainingData)

	println(s"Model coefficients: ${model.coefficients} " +
		s"\nModel intercept: ${model.intercept}")
	println(s"Model current threshold = ${model.getThreshold}")


	val trainingSummary: LogisticRegressionTrainingSummary = model.summary

	val binarySummary: BinaryLogisticRegressionSummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]
	println(s"binarySummary.fMeasureByThreshold = ${binarySummary.fMeasureByThreshold}")


	val roc: DataFrame = binarySummary.roc
	println(s"binarySummary.roc = ${binarySummary.roc}")
	roc.show()

	println(s"Area under the ROC curve = ${binarySummary.areaUnderROC}")

	def isItBetterThanRandom(model: LogisticRegressionModel) = {
		val trainingSummary = model.summary

		val binarySummary: BinaryLogisticRegressionSummary =
			trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

		val auc: Double = binarySummary.areaUnderROC

		auc > 0.5
	}

	println(s"isItBetterThanRandom(model) = ${isItBetterThanRandom(model)}")

	val fMeasure: DataFrame = binarySummary.fMeasureByThreshold
	println(s"binarySummary.fMeasureByThreshold = ${fMeasure}")

	val maxFMeasure: Double = fMeasure.select(max("F-Measure")).head().getDouble(0)
	println(s"maxFMeasure = $maxFMeasure")

	val bestThreshold: Double = fMeasure.where($"F-Measure" === maxFMeasure)
		.select("threshold").head().getDouble(0)
	// REPL
	//val bestThreshold: Double = fMeasure.where($"F-Measure" === maxFMeasure).select("threshold").head().getDouble(0)
	println(s"bestThreshold (by f-measure) = ${bestThreshold}")

	model.setThreshold(bestThreshold)

	// TODO see meaning of transform here - Transformer class? How does it happen?
	val predictions: DataFrame = model.transform(testingData)
	println(s"predictions")
	predictions.show(5)

	val evaluator: BinaryClassificationEvaluator = new BinaryClassificationEvaluator()
		.setLabelCol("label")
		.setRawPredictionCol("rawPrediction")
		.setMetricName("areaUnderPR")
	// REPL
	// val evaluator: BinaryClassificationEvaluator = new BinaryClassificationEvaluator().setLabelCol("label").setRawPredictionCol("rawPrediction").setMetricName("areaUnderPR")

	// Area under Precision-Recall curve
	val areaUnderPR: Double = evaluator.evaluate(predictions)


	def betterThanRandom(area: Double): Boolean = {
		area > 0.5
	}

	println("Area under Precision-Recall Curve " + areaUnderPR)

	case class Results(model: LogisticRegressionModel,
				    evaluatedTime: DateTime,
				    areaUnderTrainingROC: Double,
				    areaUnderTestingPR: Double)

	case class ResultsAlternate(modelId: Long,
						   evaluatedTime: DateTime,
						   precision: Double,
						   recall: Double)

}

