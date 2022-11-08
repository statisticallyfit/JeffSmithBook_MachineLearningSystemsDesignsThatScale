package com.reactivemachinelearning

import com.github.nscala_time.time.Imports._
import org.apache.spark.ml.classification.{BinaryLogisticRegressionSummary, LogisticRegression, LogisticRegressionModel, LogisticRegressionTrainingSummary}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.max

object Evaluation extends App {


  val session: SparkSession = SparkSession.builder.appName("Fraud Model").getOrCreate()
  import session.implicits._

  val data: DataFrame = session.read.format("libsvm").load("src/main/resources/sample_libsvm_data.txt")

  val Array(trainingData, testingData) = data.randomSplit(Array(0.8, 0.2))

  val learningAlgo: LogisticRegression = new LogisticRegression()

  val model: LogisticRegressionModel = learningAlgo.fit(trainingData)

  println(s"Model coefficients: ${model.coefficients} Model intercept: ${model.intercept}")


  val trainingSummary: LogisticRegressionTrainingSummary = model.summary

  val binarySummary: BinaryLogisticRegressionSummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

  val roc: DataFrame = binarySummary.roc

  roc.show()

  println(s"Area under the ROC curve ${binarySummary.areaUnderROC}")

  def betterThanRandom(model: LogisticRegressionModel) = {
    val trainingSummary = model.summary

    val binarySummary: BinaryLogisticRegressionSummary = trainingSummary.asInstanceOf[BinaryLogisticRegressionSummary]

    val auc: Double = binarySummary.areaUnderROC

    auc > 0.5
  }

  betterThanRandom(model)

  val fMeasure: DataFrame = binarySummary.fMeasureByThreshold

  val maxFMeasure: Double = fMeasure.select(max("F-Measure")).head().getDouble(0)

  val bestThreshold: Double = fMeasure.where($"F-Measure" === maxFMeasure)
    .select("threshold").head().getDouble(0)

  model.setThreshold(bestThreshold)

  // TODO see meaning of transform here - Transformer class? How does it happen?
  val predictions: DataFrame = model.transform(testingData)

  predictions.show(5)

  val evaluator: BinaryClassificationEvaluator = new BinaryClassificationEvaluator()
    .setLabelCol("label")
    .setRawPredictionCol("rawPrediction")
    .setMetricName("areaUnderPR")

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

