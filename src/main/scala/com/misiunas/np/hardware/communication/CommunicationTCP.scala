package com.misiunas.np.hardware.communication

import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
import java.net.Socket

import akka.actor.{Actor, Props}
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec

/**
 * Simple TCP command communicator with AKKA
 *
 * Created by kmisiunas on 15-08-02.
 */
class CommunicationTCP(val socket: Socket, val maxReadWait: Int) extends Actor
  with akka.actor.ActorLogging with Communication {

  // Variables

  import Communication._

  private var inputStream: InputStreamReader = null
  private var inFromServer: BufferedReader = null // Java like, but robust(ish)
  private var outToServer: DataOutputStream =  null


  // AKKA Actor

  override def preStart(): Unit = {
    inputStream = new InputStreamReader(socket.getInputStream())
    inFromServer = new BufferedReader(inputStream)
    outToServer = new DataOutputStream(socket.getOutputStream())
    log.info("Opened TCP connection @ " + socket.getInetAddress.getHostAddress)
  }

  override def postStop(): Unit = { // be clean
    inFromServer.close()
    inputStream.close()
    outToServer.close()
    log.info("Closed TCP connection @ "+socket.getInetAddress.getHostAddress)
  }

  // Lets do some actual work!

  /** send message */
  private def send(msg: String): Unit = outToServer.writeBytes(msg + "\n")

  /** clear buffer for new messgaes */
  @tailrec
  private def clearBuffer(): Unit = if( inFromServer.ready() ) { inFromServer.read(); clearBuffer() }

  /** attempts to read specified number of lines or times out */
  private def read(lines: Int): List[String] = {
    val deadline = System.currentTimeMillis() + maxReadWait
    /** recursively read by bite and store results*/
    @tailrec
    def buildString(current: String = ""): String = {
      if(current.count(_=='\n') == lines)
        current
      else if(inFromServer.ready())
        buildString(current + inFromServer.read().toChar) // read as fast as it can!!!
      else if(deadline < System.currentTimeMillis())
          throw new Exception("Timeout while waiting for "+ lines +
            " response(s) from TCP connection. So far got: '"+current+
            "'You could increase 'piezo.tcp.readMaxWait'?")
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


object CommunicationTCP {


  // communication


  // initialisation

  def props(socket: Socket, maxReadWait: Int) =  Props(new CommunicationTCP(socket, maxReadWait) )

  // pre configured ones

  def propsForPiezoStage(): Props = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("piezo.tcp.ip"), conf.getInt("piezo.tcp.port"))
    val maxReadWait = ConfigFactory.load.getInt("piezo.tcp.readMaxWait")
    props(socket, maxReadWait)
  }

  def propsForADCControls(): Props = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("adc.control.tcp.ip"), conf.getInt("adc.control.tcp.port"))
    val maxReadWait = ConfigFactory.load.getInt("adc.control.tcp.readMaxWait")
    props(socket, maxReadWait)
  }

}
