package com.misiunas.np.essential

import akka.actor.ActorRef
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * # represents a process that locks others in a queue
 *
 * Mostly boiler code
 *
 * Created by kmisiunas on 15-09-04.
 */
trait DeviceProcess[A] {

  /** method to be implemented to determine a process */
  protected def process: Option[A]

  private var xyzRaw: ActorRef = null
  private var amplifierRaw: Amplifier = null

  protected def xyz: ActorRef =
    if(xyzRaw != null) xyzRaw else throw new Exception("Process not initialised correctly: xyz missing")
  protected def amplifier: Amplifier =
    if(amplifierRaw != null) amplifierRaw else throw new Exception("Process not initialised correctly: amplifier missing")

  /** method for running the process */
  final def start(xyz: ActorRef, amplifier: Amplifier): Option[A] = {
    this.xyzRaw = xyz
    this.amplifierRaw = amplifier
    //todo: logging
    process
  }

}
