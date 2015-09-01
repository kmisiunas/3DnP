package com.misiunas.np.hardware.adc

import akka.actor.{Actor, Props}
import com.misiunas.np.hardware.adc.input.TCPStreamReader

/**
 * Created by kmisiunas on 15-08-17.
 */
class TestTCP extends Actor {

  val tcpRead = context.actorOf(TCPStreamReader.props(), "tcpTest")

  override def receive: Receive = {
    case s: String => println(s);
  }
}

object TestTCP {
  def props(): Props = Props(new TestTCP() )
}