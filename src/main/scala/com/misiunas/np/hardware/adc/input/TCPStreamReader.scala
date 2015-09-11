package com.misiunas.np.hardware.adc.input

import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
import java.net.Socket

import akka.actor.{Actor, Props}
import com.misiunas.np.hardware.TCPSimple
import TCPSimple.{TCPAsk, TCPReply}
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec

/**
 * TCP reader of incoming data
 *
 * Design:
 * - simplicity
 * - send data as soon as possible
 * - break up in lines
 *
 * Created by kmisiunas on 15-08-02.
 */
class TCPStreamReader extends Actor with akka.actor.ActorLogging {

  // Variables

  import TCPStreamReader._

  import context._

  import scala.concurrent.duration._

  private val maxReadWait = ConfigFactory.load.getInt("piezo.tcp.readMaxWait")

  private var inputStream: InputStreamReader = null
  private var inFromServer: BufferedReader = null // Java like, but robust(ish)
 // private var outToServer: DataOutputStream =  null


  // AKKA Actor

  override def preStart(): Unit = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("adc.input.tcp.ip"), conf.getInt("adc.input.tcp.port"))
    inputStream = new InputStreamReader(socket.getInputStream())
    inFromServer = new BufferedReader(inputStream)
    log.info("Opened incoming ADC data connection via TCP")
    //system.scheduler.scheduleOnce(10 millis, self, "tick")
    system.scheduler.schedule(10 millis, 5 millis, self, "tick")
  }

  override def postStop(): Unit = { // be clean
    inFromServer.close()
    inputStream.close()
    log.info("Closed incoming ADC data connection via TCP")
  }

  // Lets do some actual work!


  /** clear buffer for new messgaes */
  @tailrec
  private def clearBuffer(): Unit = if( inFromServer.ready() ) { inFromServer.read(); clearBuffer() }

  /** attempts to read specified number of lines or times out */
  private def readLine(): String = inFromServer.readLine()


  override def receive: Receive = {
    case "tick" =>
      //log.debug("<tick>")
      try {
        if(inFromServer.ready()) context.parent ! readLine()
      } catch {
        case e: Exception => log.error(e, "Failed receiving ADC data via TCP")
      }
      //system.scheduler.scheduleOnce(5 millis, self, "tick")
  }

}


object TCPStreamReader {

  def props(): Props = Props(new TCPStreamReader() )

}
