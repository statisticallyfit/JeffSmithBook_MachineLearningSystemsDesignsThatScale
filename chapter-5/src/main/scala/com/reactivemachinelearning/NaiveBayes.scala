package com.reactivemachinelearning


trait FeatureType[V] {
  val name: String
}

trait Feature[V] extends FeatureType[V] {
  val value: V
}

trait Label[V] extends Feature[V]

case class BooleanFeature(name: String, value: Boolean) extends Feature[Boolean]

case class BooleanLabel(name: String, value: Boolean) extends Label[Boolean]

case class BooleanInstance(features: Set[BooleanFeature], label: BooleanLabel)

class NaiveBayesModel(instances: List[BooleanInstance]) {

  val trueInstances: List[BooleanInstance] = instances.filter(i => i.label.value)
  println(s"trueInstances = $trueInstances")
  trueInstances.foreach(println(_))

  // TODO where
  // P(V) (law of total probabiliity)
  //   = P(F, M) + P(G, M) + P(C, M)
  //   =  P(M) * P(F | M) + P(M) * P(G | M) + P(M) * P(C | M)
  ////////////////////    = P(F) * P(M | F) + P(G) * P(M | G) + P(C) * P(M | C)

  // NOTE --- P(M) = overall probability of a match
  // renamed from: probabilityTrue
  val pM_probabilityOfMatch: Double = trueInstances.size.toDouble / instances.size
  println(s"pM_probabilityOfMatch = $pM_probabilityOfMatch")


  // makes unique feature type names (unique because may have multiple features which are same)
  val uniqueFeatureTypeNames: Set[String] = instances.flatMap(i => i.features.map(f => f.name)).toSet
  println(s"uniqueFeatureTypeNames = $uniqueFeatureTypeNames")


  // NOTE Building up probabilities in a list of each feature, given a match
  // NOTE ---  P(F | M), P(C | M), P(G | M)
  // renamed from: featureProbabilities
  val pFM_pGM_pCM_probabilityOfFeaturesGivenMatch: List[Double] = uniqueFeatureTypeNames.toList.map { // map over all distinct feature names
    (featureType: String) =>
      trueInstances.map { i => // TODO why need TRUE class labels?
        i.features.filter { f =>
          // NOTE conditional on match = true, just puts all the same features together (all true "food"s, all true "goOut"s, all true "cubs"
          f.name equals featureType // matches true instance feature name to one in the original list of features
        }.count { // Counts a feature value if it is true
          f => f.value
        }
      }.sum.toDouble / trueInstances.size // sum all positive examples, divide by total num of true instances
  }

  // TESTING understanding of the inner pieces logic of the above calculation
  val trueInstancesGrouped: List[List[Set[BooleanFeature]]] = uniqueFeatureTypeNames.toList.map{
    (featureType: String) =>
      trueInstances.map{ i =>
        i.features.filter { f =>
          f.name	equals featureType
        }
      }
  }
  val check_trueInstancesGrouped: List[List[Set[BooleanFeature]]] = List(
    List(
      Set(BooleanFeature("food",true)),
      Set(BooleanFeature("food",false)),
      Set(BooleanFeature("food",true)),
      Set(BooleanFeature("food",false))
    ),
    List(
      Set(BooleanFeature("goOut",true)),
      Set(BooleanFeature("goOut",false)),
      Set(BooleanFeature("goOut",false)),
      Set(BooleanFeature("goOut",true))
    ),
    List(
      Set(BooleanFeature("cubs",true)),
      Set(BooleanFeature("cubs",true)),
      Set(BooleanFeature("cubs",true)),
      Set(BooleanFeature("cubs",true))
    )
  )
  assert(trueInstancesGrouped == check_trueInstancesGrouped, "Test 1 - check_trueInstancesGrouped")

  // TESTING
  val countTrueInstancesGrouped: List[List[Int]] = uniqueFeatureTypeNames.toList.map{
    (featureType: String) =>
      trueInstances.map{ i =>
        i.features.filter { f =>
          f.name	equals featureType
        }.count(booleanFeature => booleanFeature.value)
      }
  }
  val check_countTrueInstancesGrouped: List[List[Int]] = List(List(1, 0, 1, 0), List(1, 0, 0, 1), List(1, 1, 1, 1))

  assert(countTrueInstancesGrouped == check_countTrueInstancesGrouped, "Test 2 - check_countTrueInstancesGrouped")

  // TESTING
  val sumTrueInstancesGrouped: List[Int] = uniqueFeatureTypeNames.toList.map{
    (featureType: String) =>
      trueInstances.map{ i =>
        i.features.filter { f =>
          f.name	equals featureType
        }.count(booleanFeature => booleanFeature.value)
      }.sum
  }
  val check_sumTrueInstancesGrouped: List[Int] = List(2,2,4)

  assert(sumTrueInstancesGrouped == check_sumTrueInstancesGrouped, "Test 3 - check_check_sumTrueInstancesGrouped")


  // Zipping the feature names corresponding to its calculated probability (F, P(F|M)), (G, P(G | M)), (C, P(C|M))
  val featureProbsAndFeaturePairs: List[(String, Double)] = uniqueFeatureTypeNames.toList.zip(pFM_pGM_pCM_probabilityOfFeaturesGivenMatch)
  println(s"pFM_pGM_pCM_probabilityOfFeaturesGivenMatch = $featureProbsAndFeaturePairs")




  // NOTE --- Numerator (naive bayes, assuming F, G, C are independent) = P(M) * P(F | M) * P(G | M) * P(C | M)
  val numeratorNum: Double = pFM_pGM_pCM_probabilityOfFeaturesGivenMatch.reduceLeft(_ * _) * pM_probabilityOfMatch


  // TODO is this P(V), which uses the law of total probability ?
  // Probability P (V)
  //  = P(V)
  //  = P(F, M) + P(G, M) + P(C, M)
  //  = P(M) * P(F | M) + P(M) * P(G | M) + P(M) * P(C | M)
  def probabilityFeatureVector(features: Set[BooleanFeature]): Double = {
    val matchingInstances = instances.count(i => i.features == features)
    matchingInstances.toDouble / instances.size // probability of match
  }
  // TODO why doesn't this calculation work out to be the same as above?
  // FoldLeft way:
  // pFM_pGM_pCM_probabilityOfFeaturesGivenMatch.map(p => (pM_probabilityOfMatch, p)).foldLeft(0.0){case (accProb:Double,
  // (pm: Double, p: Double)) => accProb + pm * p}
  // Another way (map/sum):
  // pFM_pGM_pCM_probabilityOfFeaturesGivenMatch.map(p => pM_probabilityOfMatch * p).sum

  // Given a new feature vector, calculate the denominator and divide the numerator by it
  // TODO confirm - Bayesian update formula??
  // P(M | F, G, C)
  def predict(features: Set[BooleanFeature]) = {
    numeratorNum / probabilityFeatureVector(features)
  }

}
