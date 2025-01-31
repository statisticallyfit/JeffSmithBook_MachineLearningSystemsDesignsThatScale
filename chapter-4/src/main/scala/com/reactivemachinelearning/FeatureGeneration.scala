package com.reactivemachinelearning

import org.apache.spark.ml.{Pipeline, PipelineModel, PipelineStage, Transformer}
import org.apache.spark.ml.feature.{ChiSqSelector, HashingTF, Tokenizer}
import org.apache.spark.ml.param.Param
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.stat.Statistics
import org.apache.spark.mllib.stat.test.ChiSqTestResult
import org.apache.spark.rdd.RDD
import org.apache.spark.sql
import org.apache.spark.sql.functions.{col, explode}
import org.apache.spark.sql.{Dataset, SparkSession}

import scala.util.Random

object FeatureGeneration extends App {

  // setup
  val session: SparkSession = SparkSession.builder()
       .master("local[1]")
       .appName("Feature Generation")
       .getOrCreate()
  // For REPL
  // val session: SparkSession = SparkSession.builder().master("local[1]").appName("Feature Generation").getOrCreate()

  import session.implicits._


  case class Squawk(id: Int, text: String)

  // Input data: Each row is a 140 character or less squawk
  val squawks: Seq[Squawk] = Seq(Squawk(123, "Clouds sure make it hard to look on the bright side of things."),
    Squawk(124, "Who really cares who gets the worm?  I'm fine with sleeping in."),
    Squawk(125, "Why don't french fries grow on trees?"))

  val squawksDF: sql.DataFrame = session.createDataFrame(squawks).toDF("squawkId", "squawk")
  println("squawksDF.show()")
  squawksDF.show()
  println(s"squawks snippets = \n${squawksDF.select(col("squawk"))
       .collect.map(row => row.toSeq)
       .toList.mkString("\n")}")


  val tokenizer: Tokenizer = new Tokenizer().setInputCol("squawk").setOutputCol("words")
  println(s"tokenizer = $tokenizer")
  println(s"tokenizer.uid = ${tokenizer.uid}")
  println(s"tokenizer.inputCol = ${tokenizer.inputCol.name}")
  println(s"tokenizer.getInputCol = ${tokenizer.getInputCol}")
  println(s"tokenizer.outputCol = ${tokenizer.outputCol.name}")
  println(s"tokenizer.getOutputCol = ${tokenizer.getOutputCol}")



  // Executes the tokenizer and populates the words column in a dataframe
  val tokenized: sql.DataFrame = tokenizer.transform(squawksDF)
  println(s"tokenized = tokenizer.transform(squawksDF)")
  tokenized.show()

  tokenized.select(col("squawk"), explode(col("words"))).show(false)

  // words column is called a "feature"
  println("tokenized.select(\"words\", \"squawkId\").show()")
  tokenized.select("words", "squawkId").show()
  tokenized.select(col("words")).collect.map(r => r.toSeq.toList.head).foreach(println(_))


  /// -------------------------------------------------------------------------------------


  /**
   * Creating schema for feature extraction
   * @tparam V
   */
  // Defines base trait for all types of features
  // Type parameter to hold the type of values generated by the feature
  trait FeatureType[V] {
    val name: String
  } // NOTE instead of type param, book has type V here on inside - meaning / differences?


  // Defines base trait for all features as an extension of feature types
  trait Feature[V] extends FeatureType[V] {
    val value: V
  }


  // Defines case class for features consisting of word sequences
  case class WordSequenceFeature(name: String, value: Seq[String]) extends Feature[Seq[String]] {
    //type V = Seq[String] // NOTE: old code when Feature trait did not have parameter
  }


  // Maps over rows and applies a function to each
  val wordsFeatures: Dataset[WordSequenceFeature] = tokenized
       .select("words") // selects a words column from the dataframe
       .map(row => //- creates an instance of WordSequenceFeature called "words"
         WordSequenceFeature("words",
           row.getSeq[String](0))) // gets extracted words out of a row

  // REPL
  // val wordsFeatures = tokenized.select("words").map(row => WordSequenceFeature("words", row.getSeq[String](0)))

  // TODO search spark for how to show the array that is in the row in longitudinal format so I can see every word
  //  (because now some words don't show: Array(1, 2, ...))
  println("wordsFeatures.show()")
  wordsFeatures.show()


  /**
   * Way 1 - (example feature transformation) to calculate term frequencies
   * Way 1 - using hashingTF and its transform() method directly directly
   */
  // Instantiation of a class to calculate term frequencies
  val hashingTF: HashingTF = new HashingTF()
       .setInputCol("words") // defines an input column to read from when consuming DataFrames
       .setOutputCol("termFrequencies") // defines an output column to put term frequences in.
  // NOTE: this.type from inside HashingTF means the HashingTF type

  // FOR REPL
  //val hashingTF: HashingTF = new HashingTF().setInputCol("words").setOutputCol("termFrequencies")

  // TODO: where / how does HashingTF calculate the term frequency???
  println(s"hashingTF = $hashingTF")

  // Executes the feature transformation
  // CAUGHTUP find out how the term frequency is calculated (from this abstract transform() function)
  val tokenizedHTF: sql.DataFrame = hashingTF.transform(tokenized)
  println("val tokenizedHTF: sql.DataFrame = hashingTF.transform(tokenized)")
  println("tokenizedHTF.show")
  tokenizedHTF.show()

  // prints term frequencies for inspection
  // TODO debug DEBUG where was this column created?
  println("tokenizedHTF.select(\"termFrequencies\").show()")
  tokenizedHTF.select("termFrequencies").show()

  // CAUGHTUP print the term frequencies as exploded - but how to deal with number in front?
  // Prints term frequencies more visibly to see what is contained in each row
  println("exploded term frequencies lines:")
  tokenizedHTF.select(col("termFrequencies")).collect.map(r => r.toSeq.toList.head).foreach(println(_))

  // Prints the words in expanded form (expanded array)
  println("Print words array exploded form: ")
  println(s"num sentences = ${tokenizedHTF.count()}")
  tokenizedHTF.select(col("squawk"), explode(col("words"))).show(false)




  /**
   * Way 2 - (example feature transformation) - to ccalculate term frequencies
   * Way2 = using pipeline
   */
  val pipeline: Pipeline = new Pipeline().setStages(Array(tokenizer, hashingTF)) // sets the stages of the pipeline (first tokenizer DF, then the hash
  // term freq calculation object)
  println(s"pipeline (tokenizer, hashingTF)")
  println(s"pipeline object = $pipeline")
  val pipelineStages: Param[Array[PipelineStage]] = pipeline.stages
  println(s"pipeline.stages = ${pipeline.stages}")
  val pipelineGetStages: Array[PipelineStage] = pipeline.getStages
  println(s"pipeline.getStages = ${pipeline.getStages}")
  val pipelineParams: Array[Param[_]] = pipeline.params
  println(s"pipeline.params = ${pipeline.params}")

  // Executes the pipeline
  val pipelineHashed: PipelineModel = pipeline.fit(squawksDF)
  println(s"pipelineHashed = $pipelineHashed")
  val pipelineModelStages: Array[Transformer] = pipelineHashed.stages
  println(s"pipelineHashed.stages = ${pipelineHashed.stages}")
  // Prints the type of the result of the pipeline, a PipeLineModel
  println(s"pipelineHashed.getClass = ${pipelineHashed.getClass}")
  //assert(pipelineHashed.getClass == PipelineModel)


  // Case class representing a numerical feature where the value is an integer
  case class IntFeature(name: String, value: Int)
       extends Feature[Int]
  // REPL
  // case class IntFeature(name: String, value: Int) extends Feature[Int]

  // Case class representing a boolean feature
  case class BooleanFeature(name: String, value: Boolean)
       extends Feature[Boolean]
  // REPL
  // case class BooleanFeature(name: String, value: Boolean) extends Feature[Boolean]

  trait Named {
    def name(inputFeature: Feature[_]): String = {
      inputFeature.name + "-" + Thread.currentThread.getStackTrace()(3).getMethodName
    }
  }

  object Binarizer extends Named {
    // Binarize, but using a Name abstraction
    def binarize(feature: IntFeature, threshold: Double): BooleanFeature = {
      BooleanFeature(name(feature), feature.value > threshold)
    }
  }


  // Function that transforms a numeric Integer Feature and threshold to Boolean feature.
  // NOTE: is an example of a reusable TRANSFORM function
  def binarize(feature: IntFeature, threshold: Double): BooleanFeature = {
    BooleanFeature("binarized-" + feature.name, feature.value > threshold)
    // Adds the name of the transform function to the resulting feature name (above, name is fromName
    // abstraction, and here name is from binarize() function)
  }




  // Constant, defining the cutoff for a squawker to be super
  val SUPER_THRESHOLD = 1000000

  // Raw numbers of followers for the squirrel animal and sloth animal
  val squirrelFollowers = 12
  val slothFollowers = 23584166

  // Integer Feature representing the number of followers (encapsulating int in IntFeature)
  val squirrelFollowersFeature: IntFeature = IntFeature("followers", squirrelFollowers)
  val slothFollowersFeature: IntFeature = IntFeature("followers", slothFollowers)

  // Boolean feature indicating whether the animals are a super squawker or not
  val squirrelIsSuper: BooleanFeature = binarize(squirrelFollowersFeature, SUPER_THRESHOLD)
  println(s"simple binarize: squirrelIsSuper = $squirrelIsSuper")
  val slothIsSuper: BooleanFeature = binarize(slothFollowersFeature, SUPER_THRESHOLD)
  println(s"simple binarize: slothIsSuper = $slothIsSuper")

  assert(squirrelIsSuper.value == false)
  assert(slothIsSuper.value == true)


  println("Organized binarize: ")
  println("Binarize it:" + Binarizer.binarize(squirrelFollowersFeature, SUPER_THRESHOLD))




  /// -------------------------------------------------------------------------------------

  /**
   * Creating concept labels from features
   *
   * Feature --> Concept label
   *
   * [Listing 4.8]
   *
   * @tparam V
   */

  // Label as SUBTYPE of Feature
  // Essentially an annotator saying when using feature as a concept label
  trait ConceptLabel[V] extends Feature[V]

  case class BooleanConceptLabel(name: String, value: Boolean) extends ConceptLabel[Boolean]

  // Simple conversion function from BooleanFeature to BooleanLabel
  def toBooleanConceptLabel(feature: BooleanFeature): BooleanConceptLabel = {
    BooleanConceptLabel(feature.name, feature.value)
  }

  // Converts super squawker feature values into concept labels (feature -> concept label)
  val squirrelLabel: BooleanConceptLabel = toBooleanConceptLabel(squirrelIsSuper)
  val slothLabel: BooleanConceptLabel = toBooleanConceptLabel(slothIsSuper)

  println("Seq(squirrelLabel, slothLabel).foreach(println)")
  Seq(squirrelLabel, slothLabel).foreach(println)





  /// -------------------------------------------------------------------------------------


  /**
   * Feature selection
   */
  import org.apache.spark.mllib.linalg.{Vector => VectorsMLLibType}
  import org.apache.spark.mllib.linalg.{Vectors => VectorMLLib}
  import org.apache.spark.ml.linalg.{Vector => VectorsMLType}
  import org.apache.spark.ml.linalg.{Vectors => VectorML}

  // Hardcodes some synthetic feature and concept label data
  val instances: Seq[(Int, VectorsMLType, Double)] = Seq(
    (123, VectorML.dense(0.2, 0.3, 16.2, 1.1), 0.0),
    (456, VectorML.dense(0.1, 1.3, 11.3, 1.2), 1.0),
    (789, VectorML.dense(1.2, 0.8, 14.5, 0.5), 0.0)
  )

  // Added by @statisticallyfit for below
  def convertMLToMLLibVec(given: VectorsMLType): VectorsMLLibType = {
    VectorMLLib.dense(given.toArray)
  }

  val featuresName: String = "features" // name for feature column
  val conceptLabelName: String = "isSuper" // name for label column

  // From sample instances, create data frame that contains the instances
  val instancesDF: sql.DataFrame = session
       .createDataFrame(instances) // Creates a dataframe from the instances collection
       .toDF("id", featuresName, conceptLabelName) // Sets the name of each column in the dataframe
  // REPL
  //val instancesDF: sql.DataFrame = session.createDataFrame(instances).toDF("id", featuresName, conceptLabelName)

  println("instancesDF.show()")
  instancesDF.show()

  /**
   * Apply chi-squared test to rank the impact of each feature on the concept label
   * Called Feature Importance.
   * Less important features are filtered out.
   * Show how to select the K = 2 most important features from the feature vectors.
   */
  val K: Int = 2

  val selector: ChiSqSelector = new ChiSqSelector()
       .setNumTopFeatures(K) //sets number of features to K
       .setFeaturesCol(featuresName) // sets the column where the features are
       .setLabelCol(conceptLabelName) // ses the column where the concept labels are
       .setOutputCol("selectedFeatures") // sets the column to place results, the selected features
  // REPL
  //val selector: ChiSqSelector = new ChiSqSelector().setNumTopFeatures(K).setFeaturesCol(featuresName).setLabelCol(conceptLabelName).setOutputCol("selectedFeatures")

  println(s"ChiSqSelector = $selector")
  println(s"selector.params.map(_.name) = ${selector.params.map(_.name)}")

  val selectedFeatures: sql.DataFrame = selector
       .fit(instancesDF) // `Fits a chi-squared model to the data
       .transform(instancesDF) // selects the most important features and returns a new data frame.

  // REPL
  // val selectedFeatures: sql.DataFrame = selector.fit(instancesDF).transform(instancesDF)

  println(s"selectedFeatures.show()")
  selectedFeatures.show()



  val labeledPoints: RDD[LabeledPoint] = session.sparkContext.parallelize(instances.map({
    case (id, features, label) =>
      // Added by @statisticallyfit - fixed error here in book because this function LabeledPoint requires MLLib
      // Vector not the ML vector, which is what the chi-square selector required.
      LabeledPoint(label = label, features = convertMLToMLLibVec(features))
  }))
  println(s"Labeled points: ")
  labeledPoints.toDF().show

  val iterableLabeledPoints: List[Seq[Any]] = labeledPoints.toDF().collect.toList.map(r => r.toSeq)
  println(s"labeled points in array form = $iterableLabeledPoints")


  println("chi-squared p-value results in sorted order")
  /*val sorted: Array[Double] = Statistics.chiSqTest(labeledPoints)
       .map(result => result.pValue)
       .sorted*/
  // REPL
  //val sorted: Array[Double] = Statistics.chiSqTest(labeledPoints).map(result => result.pValue).sorted
  val sortedPairs: Array[(Seq[Any], Double)] = Statistics.chiSqTest(labeledPoints).zip(iterableLabeledPoints).map{
    case(chiSqTestResult: ChiSqTestResult, labeledPoint: Seq[Any]) =>
      (labeledPoint, chiSqTestResult.pValue) // pairing them together to see from which labeled point the
    // p-value came from
  }.sortBy(_._2)

  println(s"sortedPairs = $sortedPairs")
  sortedPairs.foreach(println)


  def validateSelection(labeledPoints: RDD[LabeledPoint], topK: Int, cutoff: Double): Boolean = {
    val pValues: Array[Double] = Statistics.chiSqTest(labeledPoints)
         .map(result => result.pValue)
         .sorted
    pValues(topK) < cutoff
  }


  // --------------------------------------------------------------------------------------------------------------



  /**
   * Feature Generation
   *
   */

  trait Generator[V] {

    def generate(squawk: Squawk): Feature[V]

  }

  object SquawkLengthCategory extends Generator[Int] {

    val ModerateSquawkThreshold = 47
    val LongSquawkThreshold = 94

    private def extract(squawk: Squawk): IntFeature = {
      IntFeature("squawkLength", squawk.text.length)
    }

    private def transform(lengthFeature: IntFeature): IntFeature = {
      val squawkLengthCategory = lengthFeature match {
        case IntFeature(_, length) if length < ModerateSquawkThreshold => 1
        case IntFeature(_, length) if length < LongSquawkThreshold => 2
        case _ => 3
      }// NOTE: returning 1,2,3 corresponds to returning index of these thresholds if they were in a list

      IntFeature("squawkLengthCategory", squawkLengthCategory)
    }

    def generate(squawk: Squawk): IntFeature = {
      transform(extract(squawk))
    }
  }


  object CategoricalTransforms {

    // NOTE: need this to be IntFeature => IntFeature because the old transform has this signature.
    def categorize(thresholds: List[Int]): (IntFeature) => IntFeature = {
      (rawFeature: IntFeature) => {
        IntFeature("categorized-" + rawFeature.name,
          thresholds.sorted
               .zipWithIndex
               .find {
                 // For each feature, check against threshold
                 case (threshold, i) => rawFeature.value < threshold
               }.getOrElse((None, -1))
               ._2) // get the index corresponding to the threshold
      }
    }
  }

  object SquawkLengthCategoryRefactored extends Generator[Int] {

    import com.reactivemachinelearning.FeatureGeneration.CategoricalTransforms.categorize

    val ModerateSquawkThreshold = 47
    val LongSquawkThreshold = 94
    val VeryLongSquawkThreshold = 141

    val Thresholds: List[Int] = List(ModerateSquawkThreshold, LongSquawkThreshold, VeryLongSquawkThreshold)

    private def extract(squawk: Squawk): IntFeature = {
      IntFeature("squawkLength", squawk.text.length)
    }

    private def transform(lengthFeature: IntFeature): IntFeature = {
      val squawkLengthCategory = categorize(Thresholds)(lengthFeature)
      IntFeature("squawkLengthCategory", squawkLengthCategory.value)
    }

    def generate(squawk: Squawk): IntFeature = {
      transform(extract(squawk))
    }
  }


  // ----------------------------------------------------------------------------------------------------

  /**
   * Feature generation composition
   */

  trait StubGenerator extends Generator[Int] {
    def generate(squawk: Squawk) = {
      IntFeature("dummyFeature", Random.nextInt())
    }
  }

  object SquawkLanguage extends StubGenerator {}

  object HasImage extends StubGenerator {}

  object UserData extends StubGenerator {}

  val featureGenerators = Set(SquawkLanguage, HasImage, UserData)


  object GlobalUserData extends StubGenerator {}

  object RainforestUserData extends StubGenerator {}

  // NOTE: A generator composed of more generators, via a set
  val globalFeatureGenerators: Set[StubGenerator] = Set(SquawkLanguage, HasImage, GlobalUserData)

  val rainforestFeatureGenerators: Set[StubGenerator] = Set(SquawkLanguage, HasImage, RainforestUserData)


  trait RainforestData {
    self =>
    require(rainforestContext(),
      s"${self.getClass} uses rainforest data outside of a rainforest context.")

    private def rainforestContext(): Boolean = {
      val environment: Option[String] = Option(System.getenv("RAINFOREST"))
      environment.isDefined && environment.get.toBoolean
    }
  }

  object SafeRainforestUserData extends StubGenerator with RainforestData {}

  val safeRainforestFeatureGenerators: Set[StubGenerator] = Set(SquawkLanguage, HasImage, SafeRainforestUserData)

  // TODO find out how it reacts when unsafe.
}

