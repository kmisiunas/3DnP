package com.misiunas.np.hardware.adc.control

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import akka.util.Timeout
import com.misiunas.np.hardware.TCPSimple
import com.misiunas.np.hardware.adc.control.DAC._
import com.misiunas.np.tools.Talkative
import akka.pattern.ask

import scala.concurrent.{Await, Future}

/**
 * # Deals with communication with LabView
 *
 * Created by kmisiunas on 15-09-02.
 */
class DACWorker (tcp: ActorRef) extends Actor with ActorLogging {


  override def receive: Receive = {
    case SetDC_V(v) =>
      if( ! sendAndConfirm(commandToJSON("dc_v", v)) )
        log.error("Could not set DC voltage on LabView")
    case SetAC_V(v) =>
      if( ! sendAndConfirm(commandToJSON("ac_v", v)) )
        log.error("Could not set AC voltage on LabView")
    case SetFrequency(f) =>
      if( ! sendAndConfirm(commandToJSON("ac_frequency", f)) )
        log.error("Could not set AC frequency on LabView")
    case SetAC(active) =>
      if( ! sendAndConfirm(commandToJSON("ac", if(active) 1.0 else 0.0 )) )
        log.error("Could not set AC on/off on LabView")
    case SetMode(mode) =>
      val set: Double = mode match {
        case ImagingElectrode => 0.0
        case DepositionElectrode => 1.0
      }
      if( ! sendAndConfirm(commandToJSON("electrode_mode", set )) )
        log.error("Could not set electrode mode on LabView")
    case Pulse(amount) =>
      if( ! sendAndConfirm(commandToJSON("pulse", amount)) )
        log.error("Could not do pulse on LabView")
  }

  def commandToJSON(command: String, value: Double) =
    "{ \"command\" : \"" + command + "\", \"value\" : "+value+" }"


  def sendAndConfirm(st: String): Boolean = {
    try {
      import scala.concurrent.duration._
      implicit val timeout = Timeout(2 seconds)
      val response: String = Await.result( tcp ? TCPSimple.TCPAsk(st) , timeout.duration).asInstanceOf[String]
      response.trim == "OK"
    } catch {
      case e: Exception =>
        log.warning("Timeout while waiting for response from LabView command input")
        false
    }
  }

}

object DACWorker {
  def props(tcp: ActorRef): Props = Props( new DACWorker(tcp) )
}
