package com.misiunas.np.hardware.communication

import java.net.Socket

import akka.actor.Props
import com.typesafe.config.ConfigFactory

/**
  * Serial communication methods
  *
  * Created by kmisiunas on 2016-07-01.
  */
trait Communication {

}

object Communication {

//  case class Ask(question: String, lines: Int = 1)
//  case class Tell(tell: String)
//  case class Reply(reply: List[String])

  case class SerialAsk(question: String, lines: Int = 1)
  case class SerialTell(tell: String)
  case class SerialReply(reply: List[String])



  // Special cases

  def propsForPiezoStage(): Props = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("piezo.tcp.ip"), conf.getInt("piezo.tcp.port"))
    val maxReadWait = ConfigFactory.load.getInt("piezo.tcp.readMaxWait")
    CommunicationTCP.props(socket, maxReadWait)
  }

  def propsForADCControls(): Props = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("adc.control.tcp.ip"), conf.getInt("adc.control.tcp.port"))
    val maxReadWait = ConfigFactory.load.getInt("adc.control.tcp.readMaxWait")
    CommunicationTCP.props(socket, maxReadWait)
  }



}