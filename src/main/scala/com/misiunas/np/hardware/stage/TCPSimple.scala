package com.misiunas.np.hardware.stage

import java.io.{BufferedReader, DataOutputStream, InputStreamReader}
import java.net.Socket

import akka.actor.{Actor, Props}
import com.typesafe.config.ConfigFactory

/**
 * Simple TCP command communicator with AKKA
 *
 * Created by kmisiunas on 15-08-02.
 */
class TCPSimple (val socket: Socket) extends Actor with akka.actor.ActorLogging {

  import TCPSimple._

  private val tcpReadWait = ConfigFactory.load.getInt("piezo.tcp.readWait")

  val inFromServer: BufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
  val outToServer: DataOutputStream =  new DataOutputStream(socket.getOutputStream())

  override def receive: Receive = {
    case TCPTell(tell) =>
      try {
        outToServer.writeBytes(tell + '\n')
      } catch {
        case e: Exception =>
          log.error(e, "TCPTell failed")
          sender ! TCPError(e.toString)
      }

    case TCPAsk(tell) =>
      try {
        //inFromServer.reset() //dont get confused with old messages
        outToServer.writeBytes(tell + '\n')
        stupidWait(tcpReadWait)
        sender ! TCPReply( read() )
      } catch {
        case e: Exception =>
          log.error(e, "TCPAsk failed")
          sender ! TCPError(e.toString)
      }
  }

  private def read(): List[String] = {
    if (inFromServer.ready())
      inFromServer.readLine() :: read()
    else
      Nil
  }

  def stupidWait(ms: Int) = {
    val t0 = System.currentTimeMillis()
    while (t0 + ms >= System.currentTimeMillis()){}
  }

}


object TCPSimple {

  def props(): Props = {
    val conf = ConfigFactory.load
    val socket = new Socket(conf.getString("piezo.tcp.ip"), conf.getInt("piezo.tcp.port"))
    Props(new TCPSimple(socket) )
  }

  // communication

  case class TCPReply(reply: List[String])
  case class TCPAsk(question: String)
  case class TCPTell(tell: String)
  case class TCPError(st: String)
}
