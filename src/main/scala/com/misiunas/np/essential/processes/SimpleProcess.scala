package com.misiunas.np.essential.processes

import akka.actor.ActorRef
import com.misiunas.np.essential.{Amplifier, DeviceProcess, ProbePosition}
import com.misiunas.np.essential.DeviceProcess.{StepResponse, Finished}

/**
 * # Intended for short tasks like parameter setters
 *
 * Created by kmisiunas on 15-09-17.
 */
class SimpleProcess (val f: (ProbePosition, Amplifier) => Unit,
                     val description: String
                      ) extends DeviceProcess {

  /** steps Must perform small actions where the process can be broke in between */
  override def step(): StepResponse = {
    f(probe, amplifier)
    Finished
  }

  override def toString(): String = "SimpleProcess("+description+")"

}


object SimpleProcess {

  /** avoid using, try using amplifier layer */
  @deprecated("avoid using, try using amplifier layer")
  def DAC(dacFn: ActorRef => Unit): SimpleProcess = {
    val f: (ProbePosition, Amplifier) => Unit =
      (xyz, amplifier) => dacFn(amplifier.dac) 
    new SimpleProcess(f, "DAC only")
  }

  /** avoid using, try using amplifier layer */
  @deprecated("avoid using, try using amplifier layer")
  def IV(ivFn: ActorRef => Unit): SimpleProcess = {
    val f: (ProbePosition, Amplifier) => Unit =
      (xyz, amplifier) => ivFn(amplifier.iv)
    new SimpleProcess(f, "IV only")
  }

  def amplifier(amplifierFn: Amplifier => Unit): SimpleProcess = {
    val f: (ProbePosition, Amplifier) => Unit =
      (xyz, amplifier) => amplifierFn(amplifier)
    new SimpleProcess(f, "Amplifier only")
  }

  def xyz(xyzFn: ActorRef => Unit): SimpleProcess = {
    val f: (ProbePosition, Amplifier) => Unit =
      (xyz, amplifier) => xyzFn(xyz.xyz)
    new SimpleProcess(f, "XYZ only")
  }
  
}