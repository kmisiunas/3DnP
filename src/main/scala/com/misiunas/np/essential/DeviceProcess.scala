package com.misiunas.np.essential

import akka.actor.ActorRef
import com.misiunas.np.essential.DeviceProcess.{ProcessResults, StepResponse, Success}
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * # represents a process that locks others in a queue
 *
 * ToDo: implement such that other processes could be easily executed from main one
 *
 * Uses Android styled lifecycle:
 * http://stackoverflow.com/questions/8515936/android-activity-life-cycle-what-are-all-these-methods-for
 *
 * Object implementation
 *
 * Created by kmisiunas on 15-09-04.
 */
trait DeviceProcess extends Serializable {

  /** steps Must perform small actions where the process can be broke in between */
  def step(): StepResponse

  // lifecycle management

  def onCreate(): Unit = {}
  def onStart(): Unit = {}
  def onResume(): Unit = {}

  def onPause(): Unit = {}
  def onStop(): ProcessResults = Success

  def onRestart(): Unit = {}

  // boiled code

  private var amplifierRaw: Amplifier = null
  private var probeRaw: ProbePosition = null

  protected def xyz: ActorRef = probe.xyz // todo: phase out
  protected def probe: ProbePosition =
    if(probeRaw != null) probeRaw else throw new Exception("Process not initialised correctly: probe missing")
  protected def amplifier: Amplifier =
    if(amplifierRaw != null) amplifierRaw else throw new Exception("Process not initialised correctly: amplifier missing")

  /** method for running the process */
  final def start(probe: ProbePosition, amplifier: Amplifier): Unit = {
    this.probeRaw = probe
    this.amplifierRaw = amplifier
    onStart()
  }

  onCreate() // todo experimental: test if this is run in implementations

}

object DeviceProcess{

  /** way of coordinating steps */
  trait StepResponse
  case object Continue extends StepResponse
  case object Finished extends StepResponse
  case class InjectProcess(process: DeviceProcess) extends StepResponse
  case class Panic(msg: String) extends StepResponse  // kill all subsequent tasks and notify the user


  trait ProcessResults
  case object Success extends ProcessResults

}
