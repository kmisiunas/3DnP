package com.misiunas.np.essential

import akka.actor.ActorRef
import com.misiunas.np.essential.DeviceProcess.ContinueQ
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * # represents a process that locks others in a queue
 *
 * ToDo: implement such that other processes could be easily executed from main one
 *
 * Mostly boiler code
 *
 * Object implementation
 *
 * Created by kmisiunas on 15-09-04.
 */
trait DeviceProcess {

  /** steps Must perform small actions where the process can be broke in between */
  def step(): ContinueQ

  def preStop(): Unit = {}

  def init(): Unit = {}

  private var xyzRaw: ActorRef = null
  private var amplifierRaw: Amplifier = null

  protected def xyz: ActorRef =
    if(xyzRaw != null) xyzRaw else throw new Exception("Process not initialised correctly: xyz missing")
  protected def amplifier: Amplifier =
    if(amplifierRaw != null) amplifierRaw else throw new Exception("Process not initialised correctly: amplifier missing")

  /** method for running the process */
  final def start(xyz: ActorRef, amplifier: Amplifier): Unit = {
    this.xyzRaw = xyz
    this.amplifierRaw = amplifier
    init()
  }

}

object DeviceProcess{

  /** way of coordinating steps */
  abstract class ContinueQ()
  case object Continue extends ContinueQ
  case object Finished extends ContinueQ

}
