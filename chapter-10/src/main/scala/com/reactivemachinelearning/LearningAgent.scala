package com.reactivemachinelearning



import scala.collection.mutable

class LearningAgent {

  import com.reactivemachinelearning.LearningAgent._

  // Creates a modifiable collection of observations about things and their likability
  val knowledge: mutable.HashMap[String, Sentiment] = new mutable.HashMap[String, Sentiment]()

  // Observes a thing and whether to like it
  def observe(thing: String, sentiment: Sentiment): Unit = {
    // Records that observation in a data structure of knowledge
    knowledge.put(thing, sentiment)
  }

  // Takes things to like as a string and returns a boolean
  def doYouLike(thing: String): Boolean = {
    // Checks for the presence of a thing in known likes and returns false if it is not known
    knowledge.getOrElse(thing, DISLIKE).asBoolean
  }



  // ADDING NEW FUNCTIONALITY

  // Dislikes are stored as vowel characters to dislike
  val learnedDislikes: mutable.HashSet[Char] = new mutable.HashSet[Char]()



  // Function to invoke the learning process
  def learn(): Unit = {

    // Set of vowels to reference
    val Vowels: Set[Char] = Set[Char]('a', 'e', 'i', 'o', 'u', 'y')

    // Iterates through all entries in the knowledge base
    knowledge.foreach({

      // Pattern matches on things and known sentiments about them
      case (thing: String, sentiment: Sentiment) => {
        // Finds vowels in a given thing (that is to be evaluated if it is to be liked or disliked)
        val thingVowels: Set[Char] = thing.toSet.filter(letterOfThing => Vowels.contains(letterOfThing))

        // If the 'thing' is disliked, add its vowels to a set of disliked vowels
        if (sentiment == DISLIKE) {
          //thingVowels.foreach(learnedDislikes.add)
          thingVowels.foreach(vowel => learnedDislikes.add(vowel))
        }

      }
    }
    )
  }

  // New function to access an alternative form of likabiliy
  def doYouReallyLike(thing: String): Boolean = {
    // Likes only things with no disliked vowels
    thing.toSet.forall(!learnedDislikes.contains(_))
  }

}

object LearningAgent {

  sealed trait Sentiment {
    def asBoolean: Boolean
  }

  case object LIKE extends Sentiment {
    val asBoolean = true
  }

  case object DISLIKE extends Sentiment {
    val asBoolean = false
  }

}

object LearningAgentTester extends App {


  import com.reactivemachinelearning.LearningAgent._

  //TODO understand if learn() gets called? to update the dislikes set?

  // TESTING: the simple observe-like capabilities

  val agent: LearningAgent = new LearningAgent()

  println(s"Knowledge = ${agent.knowledge}")
  assert(agent.knowledge == Map())

  // Training
  agent.observe("honey", LIKE)
  println(s"Knowledge AFTER (observe: honey) = ${agent.knowledge}")
  println(s"agent.learnedDislikes AFTER (honey) = ${agent.learnedDislikes}") // empty because we didn't call learn()
  assert(agent.knowledge == Map("honey" -> LIKE))
  assert(agent.learnedDislikes == Set())
  agent.observe("flowers", LIKE)
  println(s"Knowledge AFTER (observe: flowers) = ${agent.knowledge}")
  println(s"agent.learnedDislikes AFTER (flowers) = ${agent.learnedDislikes}")
  assert(agent.knowledge == Map("honey" -> LIKE, "flowers" -> LIKE))
  assert(agent.learnedDislikes == Set())
  agent.observe("mushrooms", DISLIKE)
  println(s"Knowledge AFTER (observe: mushrooms) = ${agent.knowledge}")
  println(s"agent.learnedDislikes AFTER (mushrooms) = ${agent.learnedDislikes}")
  assert(agent.knowledge == Map("honey" -> LIKE, "flowers" -> LIKE, "mushrooms" -> DISLIKE))
  assert(agent.learnedDislikes == Set())

  // Querying
  println(s"agent.doYouLike('honey') = ${agent.doYouLike("honey")}")
  println(s"agent.doYouLike('mushrooms') = ${agent.doYouLike("mushrooms")}")
  println(s"agent.doYouLike('birds') = ${agent.doYouLike("birds")}")
  assert(agent.doYouLike("honey"))
  assert(! agent.doYouLike("mushrooms"))
  assert(! agent.doYouLike("birds")) // not
  assert(agent.knowledge == Map("honey" -> LIKE, "flowers" -> LIKE, "mushrooms" -> DISLIKE))
  assert(agent.learnedDislikes == Set())

  // Training
  agent.observe("birds", LIKE) // teach
  println(s"Knowledge AFTER (observe: birds) = ${agent.knowledge}")
  assert(agent.knowledge == Map("honey" -> LIKE, "flowers" -> LIKE, "mushrooms" -> DISLIKE, "birds" -> LIKE))
  assert(agent.learnedDislikes == Set())

  // Querying
  println(s"agent.doYouLike('birds') = ${agent.doYouLike("birds")}")
  assert(agent.doYouLike("birds"))  //now like
  assert(agent.learnedDislikes == Set())


  // TESTING: the vowel-learning capabilities

  println("TESTING: the vowel-learning capabilities")

  val agent2: LearningAgent = new LearningAgent()

  println(s"Knowledge BEFORE = ${agent2.knowledge}")
  println(s"agent.learnedDislikes BEFORE = ${agent2.learnedDislikes}")
  assert(agent2.knowledge == Map())

  // Training the vowels for testing the "liking" of dogs / cats
  // Training
  agent2.observe("ants", DISLIKE)
  agent2.learn()
  println(s"Knowledge AFTER (observe/learn: ants) = ${agent2.knowledge}")
  println(s"agent.learnedDislikes AFTER (observe/learn: ants) = ${agent2.learnedDislikes}")
  assert(agent2.knowledge == Map("ants" -> DISLIKE))
  assert(agent2.learnedDislikes == Set('a'))

  agent2.observe("bats", DISLIKE)
  agent2.learn()
  println(s"Knowledge AFTER (observe/learn: bats) = ${agent2.knowledge}")
  println(s"agent.learnedDislikes AFTER (observe/learn: bats) = ${agent2.learnedDislikes}")
  assert(agent2.knowledge == Map("bats" -> DISLIKE, "ants" -> DISLIKE))
  assert(agent2.learnedDislikes == Set('a'))

  // Querying
  println(s"agent.doYouReallyLike('dogs') = ${agent2.doYouReallyLike("dogs")}")
  assert( agent2.doYouReallyLike("dogs"), "Test: dogs" )
  println(s"agent.doYouReallyLike('cats') = ${agent2.doYouReallyLike("cats")}")
  assert( ! agent2.doYouReallyLike("cats"), "Test: cats" )
  //assert(! agent.doYouReallyLike("cats")) // dislike because vowels of 'cats' are like vowels of 'ants' and 'bats'
  println(s"agent.doYouReallyLike('ice') = ${agent2.doYouReallyLike("ice")}")
  assert( agent2.doYouReallyLike("ice"), "Test: ice" )
  println(s"agent.doYouReallyLike('igloo') = ${agent2.doYouReallyLike("igloo")}")
  assert( agent2.doYouReallyLike("igloo"), "Test: igloo" )


  // Training vowels for testing the "liking" of 'yellow'
  agent2.observe("olleyoup", LIKE)
  agent2.learn()
  println(s"Knowledge AFTER (observe/learn: olleyoup) = ${agent2.knowledge}")
  println(s"agent.learnedDislikes AFTER (observe/learn: olleyoup) = ${agent2.learnedDislikes}")

  println(s"agent.doYouReallyLike('yellow') = ${agent2.doYouReallyLike("yellow")}")
  assert(agent.doYouReallyLike("yellow"))

}




