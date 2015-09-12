package com.misiunas.np.hardware.adc.control

import akka.actor.Actor.Receive
import akka.actor.{Props, Actor, ActorLogging}
import com.misiunas.np.hardware.TCPSimple
import com.misiunas.np.hardware.adc.control.DAC._

/**
 * # Send commands to Digital to Analog Converter (DAC)
 *
 * Created by kmisiunas on 15-09-02.
 */
class DAC extends Actor with ActorLogging{

  protected val tcp = context.actorOf(TCPSimple.propsForADCControls() ,"tcp")
  protected val worker = context.actorOf(DACWorker.props(tcp), "worker")

  var status: DACStatus = DACStatus( 0, 0, ac_frequency = 50, false, DAC.ImagingElectrode )
  
  override def preStart() = self ! SetStatus(status) // reset DAC

  override def receive: Receive = {
    case SetDC_V(v) =>
      val incoming = status.copy(dc_v = v)
      if(validate(incoming)) {
        worker ! SetDC_V(v)
        status = incoming
      } else {
        log.warning("DC_V = "+ v+" is outside the bounds. Not set.")
      }
    case SetAC_V(v) =>
      val incoming = status.copy(ac_v = v)
      if(validate(incoming)) {
        worker ! SetAC_V(v)
        status = incoming
      } else {
        log.warning("AC_V = "+ v+" is outside the bounds. Not set.")
      }
    case SetFrequency(f) =>
      val incoming = status.copy(ac_frequency = f)
      if(validate(incoming)) {
        worker ! SetFrequency(f)
        status = incoming
      } else {
        log.warning("AC_frequency = "+ f+" is outside the bounds. Not set.")
      }
    case SetAC(active) =>
      val incoming = status.copy(ac = active)
      worker ! SetAC(active)
      status = incoming
    case SetMode(mode) =>
      val incoming = status.copy(electrode_mode = mode)
      worker ! SetMode(mode)
      status = incoming
    case SetStatus(incoming) =>
      if(validate(incoming)) {
        // in order of importance
        self ! SetMode(incoming.electrode_mode)
        self ! SetAC(incoming.ac)
        self ! SetDC_V(incoming.dc_v)
        self ! SetFrequency(incoming.ac_frequency)
        self ! SetAC_V(incoming.ac_v)
      } else {
        log.warning("Incoming DACStatus = "+incoming + "was invalid. Not set.")
      }
    case Pulse(amount) =>
      if(amount > 0) {
        worker ! Pulse(amount)
      } else {
        log.warning("Pulse cannot have negative amount. Not sent.")
      }
    case StatusQ =>
      sender ! status

  }

  // validates inputs to prevent stupid input being set on the amplifier
  def validate(incoming: DACStatus): Boolean = {
    -1000 <= incoming.dc_v && incoming.dc_v <= 1000  &&
      0 <= incoming.ac_v && incoming.ac_v <= 500  &&
      0 < incoming.ac_frequency && incoming.ac_frequency <= 2000   // arbitrary max, but I think the sampling rate is 20000 on the device
  }

}

object DAC {

  // communication methods

  case object StatusQ

  case class SetStatus(settings: DACStatus)
  case class SetDC_V(dc_v: Double)
  case class SetAC_V(ac_v: Double)
  case class SetAC(active: Boolean)
  case class SetFrequency(frequency: Double)
  case class SetMode(electrode: ElectrodeMode)
  case class Pulse(amount: Double)

  abstract class ElectrodeMode

  case object ImagingElectrode extends ElectrodeMode
  case object DepositionElectrode extends ElectrodeMode

  // initialisation

   def props(): Props = Props( new DAC() )

}
