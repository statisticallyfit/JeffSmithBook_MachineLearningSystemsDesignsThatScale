package com.reactivemachinelearning

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{QuantileDiscretizer, VectorAssembler}
import org.apache.spark.ml.param.ParamMap
import org.apache.spark.ml.tuning.{CrossValidator, CrossValidatorModel, ParamGridBuilder}
import org.apache.spark.sql.{DataFrame, SparkSession}

object ModelPersistence extends App {

  val session: SparkSession = SparkSession.builder().master("local[1]").appName("ModelPersistence").getOrCreate()

  val data: Seq[(Int, Double, Int)] = Seq(
    (0, 18.0, 0),
    (1, 20.0, 0),
    (2, 8.0, 1),
    (3, 5.0, 1),
    (4, 2.0, 0),
    (5, 21.0, 0),
    (6, 7.0, 1),
    (7, 18.0, 0),
    (8, 3.0, 1),
    (9, 22.0, 0),
    (10, 8.0, 1),
    (11, 2.0, 0),
    (12, 5.0, 1),
    (13, 4.0, 1),
    (14, 1.0, 0),
    (15, 11.0, 0),
    (16, 7.0, 1),
    (17, 15.0, 0),
    (18, 3.0, 1),
    (19, 20.0, 0))

  val instances: DataFrame = session.createDataFrame(data)
    .toDF("id", "seeds", "label")
  // REPL
  // val instances: DataFrame = session.createDataFrame(data).toDF("id", "seeds", "label")
  println(s"instances.show")
  instances.show

  // Does binning without needing predefined boundaries between buckets, instead just lets you state the number of
  // buckets.
  val discretizer: QuantileDiscretizer = new QuantileDiscretizer()
    .setInputCol("seeds")
    .setOutputCol("discretized")
    .setNumBuckets(3)
  // REPL
  // val discretizer: QuantileDiscretizer = new QuantileDiscretizer().setInputCol("seeds").setOutputCol("discretized").setNumBuckets(3)

  println(s"quantile discretizer = $discretizer")
  println(s"discretizer.getNumBuckets = ${discretizer.getNumBuckets}")


  // VectorAssembler's purpose is to add new columns to dataframes, containing feature values wrapped in a Vector type
  val assembler: VectorAssembler = new VectorAssembler()
    .setInputCols(Array("discretized"))
    .setOutputCol("features")

  println(s"vector assembler = $assembler")
  assert(assembler.explainParams() ==
       "inputCols: input column names (current: [Ljava.lang.String;@3c0becae)" +
            "\noutputCol: output column name (default: vecAssembler_e5dd468a5d15__output, current: features)")

  val classifier: LogisticRegression = new LogisticRegression().setMaxIter(5)
  println(s"classifier = $classifier")
  println(s"classifier.getThreshold = ${classifier.getThreshold}")


  // Pipeline for composing the stages
  val pipeline: Pipeline = new Pipeline().setStages(Array(discretizer, assembler, classifier))
  println(s"pipeline.getStages = ${pipeline.getStages}")


  // Hyperparameter optimization - finding most effective parameters for the model learning
  // Tehcnique here - grid search
  val paramMaps: Array[ParamMap] = new ParamGridBuilder()
    .addGrid(classifier.regParam, Array(0.0, 0.1))
    .build()
  // REPL
  // val paramMaps: Array[ParamMap] = new ParamGridBuilder().addGrid(classifier.regParam, Array(0.0, 0.1)).build()
  println(s"classifier.getRegParam = ${classifier.getRegParam}")
  println(s"paramMaps = $paramMaps")


  val evaluator: BinaryClassificationEvaluator = new BinaryClassificationEvaluator()
  assert(evaluator.getMetricName == "areaUnderROC")
  assert(evaluator.explainParams() ==
       "labelCol: label column name (default: label)" +
            "\nmetricName: metric name in evaluation (areaUnderROC|areaUnderPR) (default: areaUnderROC)" +
            "\nrawPredictionCol: raw prediction (a.k.a. confidence) column name (default: rawPrediction)"
  )
  println(s"evaluator.extractParamMap() = ${evaluator.extractParamMap()}")


  // Cross validation = Technique for dividing data into random subsamples so model can study different portions of
  // the data.
  val crossValidator: CrossValidator = new CrossValidator()
    .setEstimator(pipeline)
    .setEvaluator(evaluator)
    .setNumFolds(2)
    .setEstimatorParamMaps(paramMaps)
  // REPL
  //val crossValidator: CrossValidator = new CrossValidator().setEstimator(pipeline).setEvaluator(evaluator).setNumFolds(2).setEstimatorParamMaps(paramMaps)
  assert(crossValidator.explainParams() ==
    "estimator: estimator for selection (current: pipeline_0e2abb06324f)" +
         "\nestimatorParamMaps: param maps for the estimator (current: [Lorg.apache.spark.ml.param.ParamMap;@3f5743d9)" +
         "\nevaluator: evaluator used to select hyper-parameters that maximize the validated metric (current: binEval_26e710baebca)" +
         "\nnumFolds: number of folds for cross validation (>= 2) (default: 3, current: 2)" +
         "\nseed: random seed (default: -1191137437)"
  )

  val cvModel: CrossValidatorModel = crossValidator.fit(instances)

  val myAbsolutePath: String = "/development/projects/statisticallyfit/github/learningspark" +
       "/JeffSmithBook_MachineLearningSystemsDesignsThatScale/chapter-7/src/main/scala/com/reactivemachinelearning/"

  val myModelName: String = "crossValidatorModel"

  cvModel.write.overwrite().save(myAbsolutePath + myModelName)
  // REPL output:
  /*
  22/11/10 17:50:54 INFO ParquetWriteSupport: Initialized Parquet WriteSupport with Catalyst schema:
{
  "type" : "struct",
  "fields" : [ {
    "name" : "numClasses",
    "type" : "integer",
    "nullable" : false,
    "metadata" : { }
  }, {
    "name" : "numFeatures",
    "type" : "integer",
    "nullable" : false,
    "metadata" : { }
  }, {
    "name" : "interceptVector",
    "type" : {
      "type" : "udt",
      "class" : "org.apache.spark.ml.linalg.VectorUDT",
      "pyClass" : "pyspark.ml.linalg.VectorUDT",
      "sqlType" : {
        "type" : "struct",
        "fields" : [ {
          "name" : "type",
          "type" : "byte",
          "nullable" : false,
          "metadata" : { }
        }, {
          "name" : "size",
          "type" : "integer",
          "nullable" : true,
          "metadata" : { }
        }, {
          "name" : "indices",
          "type" : {
            "type" : "array",
            "elementType" : "integer",
            "containsNull" : false
          },
          "nullable" : true,
          "metadata" : { }
        }, {
          "name" : "values",
          "type" : {
            "type" : "array",
            "elementType" : "double",
            "containsNull" : false
          },
          "nullable" : true,
          "metadata" : { }
        } ]
      }
    },
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "coefficientMatrix",
    "type" : {
      "type" : "udt",
      "class" : "org.apache.spark.ml.linalg.MatrixUDT",
      "pyClass" : "pyspark.ml.linalg.MatrixUDT",
      "sqlType" : {
        "type" : "struct",
        "fields" : [ {
          "name" : "type",
          "type" : "byte",
          "nullable" : false,
          "metadata" : { }
        }, {
          "name" : "numRows",
          "type" : "integer",
          "nullable" : false,
          "metadata" : { }
        }, {
          "name" : "numCols",
          "type" : "integer",
          "nullable" : false,
          "metadata" : { }
        }, {
          "name" : "colPtrs",
          "type" : {
            "type" : "array",
            "elementType" : "integer",
            "containsNull" : false
          },
          "nullable" : true,
          "metadata" : { }
        }, {
          "name" : "rowIndices",
          "type" : {
            "type" : "array",
            "elementType" : "integer",
            "containsNull" : false
          },
          "nullable" : true,
          "metadata" : { }
        }, {
          "name" : "values",
          "type" : {
            "type" : "array",
            "elementType" : "double",
            "containsNull" : false
          },
          "nullable" : true,
          "metadata" : { }
        }, {
          "name" : "isTransposed",
          "type" : "boolean",
          "nullable" : false,
          "metadata" : { }
        } ]
      }
    },
    "nullable" : true,
    "metadata" : { }
  }, {
    "name" : "isMultinomial",
    "type" : "boolean",
    "nullable" : false,
    "metadata" : { }
  } ]
}
and corresponding Parquet message type:
message spark_schema {
  required int32 numClasses;
  required int32 numFeatures;
  optional group interceptVector {
    required int32 type (INT_8);
    optional int32 size;
    optional group indices (LIST) {
      repeated group list {
        required int32 element;
      }
    }
    optional group values (LIST) {
      repeated group list {
        required double element;
      }
    }
  }
  optional group coefficientMatrix {
    required int32 type (INT_8);
    required int32 numRows;
    required int32 numCols;
    optional group colPtrs (LIST) {
      repeated group list {
        required int32 element;
      }
    }
    optional group rowIndices (LIST) {
      repeated group list {
        required int32 element;
      }
    }
    optional group values (LIST) {
      repeated group list {
        required double element;
      }
    }
    required boolean isTransposed;
  }
  required boolean isMultinomial;
}

   */

  val persistedModel: CrossValidatorModel = CrossValidatorModel.load(myAbsolutePath + myModelName)
  println(s"UID: ${persistedModel.uid}")

  assert(cvModel.uid == persistedModel.uid)

}
