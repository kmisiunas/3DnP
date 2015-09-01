package com.misiunas.np.hardware.adc

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.misiunas.np.hardware.adc.input.IV
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

/**
 * Created by kmisiunas on 15-08-31.
 */
class ADCReceiverTest  extends FlatSpec with Matchers {

  // prep work

  implicit val timeout = Timeout(2 seconds)
  def getResponse[T](f: Future[T]): T = Await.result(f, timeout.duration).asInstanceOf[T]

  // test system
  val system = ActorSystem("3DnP")

  var vi: ActorRef = null

  "IV Actor" should "connect to TCP LabView VI and read JSON messages" in {
    vi = system.actorOf( IV.props(), "vi")

    // todo
  }


  // other ters
  //  val tcpReader = system.actorOf(TestTCP.props(), "tcpReader")


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
