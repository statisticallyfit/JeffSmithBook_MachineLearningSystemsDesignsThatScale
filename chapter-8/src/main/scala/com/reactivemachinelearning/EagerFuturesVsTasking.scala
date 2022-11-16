package com.reactivemachinelearning

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalaz.concurrent.Task

object EagerFuturesVsTasking extends App {

  def doStuff(source: String): Unit = println(s"Doing $source stuff")

  val futureVersion: Future[Unit] = Future(doStuff("Future"))

  Thread.sleep(1000)

  println("After Future instantiation")

  val taskVersion: Task[Unit] = Task(doStuff("Task"))

  Thread.sleep(1000)

  println("After Task instantiation")

  taskVersion.run

}
