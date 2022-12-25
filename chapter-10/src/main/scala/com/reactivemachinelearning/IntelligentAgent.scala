package com.reactivemachinelearning

class IntelligentAgent(likes: Set[String]) {

  def doYouLike(thing: String): Boolean = {
    likes.contains(thing)
  }

}


object IntelligentAgentTester extends App {

  val aBeesLikes: Set[String] = Set("honey", "flowers")

  val agent = new IntelligentAgent(aBeesLikes)

  assert(agent.doYouLike("honey"))
  assert(agent.doYouLike("flowers"))
  assert(! agent.doYouLike("anything else"))
}
