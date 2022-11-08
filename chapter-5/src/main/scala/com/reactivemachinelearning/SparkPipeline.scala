package com.reactivemachinelearning

import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.classification.{DecisionTreeClassificationModel, DecisionTreeClassifier, RandomForestClassificationModel, RandomForestClassifier}
import org.apache.spark.ml.feature.{IndexToString, StringIndexer, StringIndexerModel, VectorIndexer, VectorIndexerModel}
import org.apache.spark.sql.{DataFrame, SparkSession}

object SparkPipeline extends App {

  val session = SparkSession.builder.appName("TimberPipeline").getOrCreate()

  val instances: DataFrame = session.read.format("libsvm").load("/Users/jeff/Documents/Projects/reactive-machine-learning-systems/chapter-5/src/main/resources/match_data.libsvm")

  val labelIndexer: StringIndexerModel = new StringIndexer()
    .setInputCol("label")
    .setOutputCol("indexedLabel")
    .fit(instances) // sets the dataframe to process

  val featureIndexer: VectorIndexerModel = new VectorIndexer()
    .setInputCol("features")
    .setOutputCol("indexedFeatures")
    .fit(instances)

  val Array(trainingData, testingData) = instances.randomSplit(Array(0.8, 0.2))

  val decisionTree: DecisionTreeClassifier = new DecisionTreeClassifier()
    .setLabelCol("indexedLabel")
    .setFeaturesCol("indexedFeatures")

  val labelConverter = new IndexToString()
    .setInputCol("prediction")
    .setOutputCol("predictedLabel")
    .setLabels(labelIndexer.labels)

  val decisionTreePipeline = new Pipeline()
    .setStages(Array(labelIndexer, featureIndexer, decisionTree, labelConverter))

  val decisionTreeFit: PipelineModel = decisionTreePipeline.fit(trainingData)

  val predictions: DataFrame = decisionTreeFit.transform(testingData)

  predictions.select("predictedLabel", "label", "features").show(1)

  val decisionTreeModel: DecisionTreeClassificationModel = decisionTreeFit.stages(2)
    .asInstanceOf[DecisionTreeClassificationModel]

  println(decisionTreeModel.toDebugString)

  val randomForest: RandomForestClassifier = new RandomForestClassifier()
    .setLabelCol("indexedLabel")
    .setFeaturesCol("indexedFeatures")

  val randomForestPipeline: Pipeline = new Pipeline()
    .setStages(Array(labelIndexer, featureIndexer, randomForest, labelConverter))

  val randomForestFit: PipelineModel = randomForestPipeline.fit(trainingData)

  val randomForestModel: RandomForestClassificationModel = randomForestFit.stages(2)
    .asInstanceOf[RandomForestClassificationModel]

  println(randomForestModel.toDebugString)

}
