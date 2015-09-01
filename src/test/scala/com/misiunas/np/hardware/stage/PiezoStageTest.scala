package com.misiunas.np.hardware.stage

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.adc.input.IV
import com.misiunas.np.tools.Wait
import org.scalatest.{Matchers, FlatSpec}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by kmisiunas on 15-08-31.
 */
class PiezoStageTest extends FlatSpec with Matchers {

  // prep work

  implicit val timeout = Timeout(2 seconds)
  def getResponse[T](f: Future[T]): T = Await.result(f, timeout.duration).asInstanceOf[T]

  // test system
  val system = ActorSystem("3DnP")

  var stage: ActorRef = null

  "PiezoStage Actor" should "connect to TCP PI instrument" in {

    // todo
//    println("Initiate Actor for connecting to Piezo Stage")
//    val piezo = system.actorOf(PiezoStage.props(), "piezo")
//
//    println("Actor alive")
//
//    println("=> ask for status")
//    var reply: Future[Any] = piezo ? PiezoStage.StatusQ
//    var res = Await.result(reply, timeout.duration).asInstanceOf[PiezoStatus]
//    println("<= got results: \n"+ res.pos)
//
//    Wait.stupid(1000)
//
//    println("=> move to new loaction: Vec(10,10,10)")
//    piezo ! PiezoStage.Move(Vec(10,10,10))
//
//    Wait.stupid(4000)
//
//    println("=> ask for Position")
//    reply = piezo ? PiezoStage.PositionQ
//    var pos = Await.result(reply, timeout.duration).asInstanceOf[Vec]
//    println("<= got results: \n"+ pos)
//
//
//    println("=> ask for status")
//    reply = piezo ? PiezoStage.StatusQ
//    res = Await.result(reply, timeout.duration).asInstanceOf[PiezoStatus]
//    println("<= got results: \n"+ res.pos)

  }


  //  "A Stack" should "pop values in last-in-first-out order" in {
  //    val stack = new Stack[Int]
  //    stack.push(1)
  //    stack.push(2)
  //    stack.pop() should be (2)
  //    stack.pop() should be (1)
  //  }

  //  it should "throw NoSuchElementException if an empty stack is popped" in {
  //    val emptyStack = new Stack[Int]
  //    a [NoSuchElementException] should be thrownBy {
  //      emptyStack.pop()
  //    }
  //  }


  // kill system
  system.shutdown()

}