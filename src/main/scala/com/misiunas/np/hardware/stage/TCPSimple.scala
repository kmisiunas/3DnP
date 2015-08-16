package com.misiunas.np.hardware.stage

import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
import java.net.Socket
import java.util.concurrent.TimeoutException

import akka.actor.{ReceiveTimeout, Actor, Props}
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec

/**
 * Simple TCP command communicator with AKKA
 *
 * Created by kmisiunas on 15-08-02.
 */
class TCPSimple (val socket: Socket) extends Actor with akka.actor.ActorLogging {

  // Variables

  import TCPSimple._

  private val maxReadWait = ConfigFactory.load.getInt("piezo.tcp.readMaxWait")

  private var inputStream: InputStreamReader = null
  private var inFromServer: BufferedReader = null // Java like, but robust(ish)
  private var outToServer: DataOutputStream =  null


  // AKKA Actor

  override def preStart(): Unit = {
    inputStream = new InputStreamReader(socket.getInputStream())
    inFromServer = new BufferedReader(inputStream)
    outToServer = new DataOutputStream(socket.getOutputStream())
    log.info("Opened connections to Piezo Stage via TCP")
  }

  override def postStop(): Unit = { // be clean
    inFromServer.close()
    inputStream.close()
    outToServer.close()
    log.info("Closed connections to Piezo Stage via TCP")
  }

  // Lets do some actual work!

  /** send message */
  private def send(msg: String): Unit = outToServer.writeBytes(msg + '\n')

  /** clear buffer for new messgaes */
  @tailrec
  private def clearBuffer(): Unit = if( inFromServer.ready() ) { inFromServer.read(); clearBuffer() }

  /** attempts to read specified number of lines or times out */
  private def read(lines: Int): List[String] = {
    val deadline = System.currentTimeMillis() + maxReadWait
    /** recursively read by bite and store results*/
    @tailrec
    def buildString(current: String = ""): String = {
      if(inFromServer.ready())
        buildString(current + inFromServer.read().toChar) // read as fast as it can!!!
      else if(current.count(_=='\n') == lines)
        current
      else if(deadline < System.currentTimeMillis())
          throw new Exception("Timeout while waiting for "+ lines +" response(s) from TCP connection. So far got: '"+current+"'You could increase 'piezo.tcp.readMaxWait'?")
      else
        buildString(current)
    }
    buildString().lines.toList.map(_.trim)
  }

  override def receive: Receive = {
    case TCPTell(tell) =>
      try {
        send(tell)
      } catch {
        case e: Exception => log.error(e, "failed sending command: "+tell)
      }
    case TCPAsk(ask, lines) =>
      try {
        clearBuffer()  // just in case there were leftovers from somewhere
        send(ask)
        sender ! TCPReply( read(lines) )
      } catch {
        case e: Exception => log.error(e, "failed asking command: "+ask)
      }
  }

}


object TCPSimple {

  def props(): Props = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("piezo.tcp.ip"), conf.getInt("piezo.tcp.port"))
    Props(new TCPSimple(socket) )
  }

  // communication

  case class TCPAsk(question: String, lines: Int = 1)
  case class TCPTell(tell: String)
  case class TCPReply(reply: List[String])

  //case class TCPError(st: String) // not used?
}
