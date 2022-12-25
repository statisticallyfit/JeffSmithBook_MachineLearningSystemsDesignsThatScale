package com.reactivemachinelearning

object ReflexAgent {

  def doYouLike(thing: String): Boolean = {
    thing == "honey"
  }

}


object ReflexAgentTester extends App {

  assert(ReflexAgent.doYouLike("honey") == true)
  assert(ReflexAgent.doYouLike("any other thing") == false)

}
