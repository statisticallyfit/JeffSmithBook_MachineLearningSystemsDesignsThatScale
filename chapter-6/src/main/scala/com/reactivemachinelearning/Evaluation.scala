package com.reactivemachinelearning

import com.github.nscala_time.time.Imports._
import org.apache.spark.ml.classification.{BinaryLogisticRegressionSummary, LogisticRegression, LogisticRegressionModel}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.max

object Evaluation extends App {


  val session = SparkSession.builder.appName("Fraud Model").getOrCreate()
  import session.implicits._

  val data = session.read.format("libsvm").load("src/main/resources/sample_libsvm_data.txt")

  val Array(trainingData, testingData) = data.randomSplit(Array(0.8, 0.2))

  val learningAlgo = new LogisticRegression()

  val model = learningAlgo.fit(trainingData)

  println(s"Model coefficients: ${model.coefficients} Model intercept: ${model.intercept}")


  val trainingSummary = model.summary

  val binarySummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

  val roc = binarySummary.roc

  roc.show()

  println(s"Area under the ROC curve ${binarySummary.areaUnderROC}")

  def betterThanRandom(model: LogisticRegressionModel) = {
    val trainingSummary = model.summary

    val binarySummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

    val auc = binarySummary.areaUnderROC

    auc > 0.5
  }

  betterThanRandom(model)

  val fMeasure = binarySummary.fMeasureByThreshold

  val maxFMeasure = fMeasure.select(max("F-Measure")).head().getDouble(0)

  val bestThreshold = fMeasure.where($"F-Measure" === maxFMeasure)
    .select("threshold").head().getDouble(0)

  model.setThreshold(bestThreshold)

  // TODO see meaning of transform here - Transformer class? How does it happen?
  val predictions = model.transform(testingData)

  predictions.show(5)

  val evaluator = new BinaryClassificationEvaluator()
    .setLabelCol("label")
    .setRawPredictionCol("rawPrediction")
    .setMetricName("areaUnderPR")

  val areaUnderPR = evaluator.evaluate(predictions)

  def betterThanRandom(area: Double) = {
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

