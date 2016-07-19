package com.misiunas.np.hardware.communication

import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
import java.net.Socket

import akka.actor.{Actor, Props}
import com.misiunas.np.hardware.communication.Communication.{SerialAsk, SerialReply, SerialTell}
import com.typesafe.config.ConfigFactory

import jssc.SerialPort
import jssc.SerialPortException

import scala.annotation.tailrec

/**
  * Created by kmisiunas on 2016-07-01.
  */
class CommunicationUSB (val port: String, val baudrate: Int, val maxReadWait: Int) extends Actor
  with akka.actor.ActorLogging with Communication {

  // Variables

  import CommunicationUSB._


  private var serial: SerialPort = null

  // AKKA Actor

  override def preStart(): Unit = {
    serial = new SerialPort(port)
    serial.openPort()
    serial.setParams(
      baudrate,
      SerialPort.DATABITS_8,
      SerialPort.STOPBITS_1,
      SerialPort.PARITY_NONE
    )
    log.info("Opened USB/Serial connection @ " + port)
  }

  override def postStop(): Unit = { // be clean
    serial.closePort()
    log.info("Closed USB/Serial connection @ "+port)
  }

  // Lets do some actual work!

  /** send message */
  private def send(msg: String): Unit = serial.writeString(msg + "\n")

  /** clear buffer for new messgaes */
  //@tailrec
  private def clearBuffer(): Unit = serial.readString()

  /** attempts to read specified number of lines or times out */
  private def read(lines: Int): List[String] = {
    val deadline = System.currentTimeMillis() + maxReadWait
    /** recursively read by bite and store results*/
    @tailrec
    def buildString(current: String = ""): String = {
      if(current.count(_=='\n') == lines)
        current
      else if(serial.getInputBufferBytesCount > 0)
        buildString(current + serial.readBytes(1).head.toChar) // read as fast as it can!!!
      else if(deadline < System.currentTimeMillis())
        throw new Exception("Timeout while waiting for "+ lines +
          " response(s) from USB connection @ "+port+". So far got: '"+current+
          "'You could increase 'maxReadWait'?")
      else
        buildString(current)
    }
    buildString().lines.toList.map(_.trim)
  }



  override def receive: Receive = {
    case SerialTell(tell) =>
      try {
        send(tell)
      } catch {
        case e: Exception => log.error(e, "failed sending command: "+tell)
      }
    case SerialAsk(ask, lines) =>
      try {
        clearBuffer()  // just in case there were leftovers from somewhere
        send(ask)
        sender ! SerialReply( read(lines) )
      } catch {
        case e: Exception => log.error(e, "failed asking command: "+ask)
      }
  }

}


object CommunicationUSB {

  def props(port: String, baudrate: Int, maxReadWait: Int) =
    Props(new CommunicationUSB(port, baudrate, maxReadWait) )

}
