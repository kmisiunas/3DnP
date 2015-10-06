package com.misiunas.np.essential.processes.minor

import breeze.numerics.log
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess.{Finished, Continue, ContinueQ}
import com.misiunas.np.hardware.stage.PiezoStage
import com.misiunas.np.tools.Talkative

/**
 * Created by kmisiunas on 15-09-18.
 */
class StepAndPulse (stepSize: Double, pulseMag: Double) extends DeviceProcess {

  private var state = "approach"

  /** steps Must perform small actions where the process can be broke in between */
  override def step(): ContinueQ = state match {
    case "approach" =>
      Talkative.getResponse(xyz, PiezoStage.MoveBy(Vec(0,0, stepSize)))
      nextStep("pulse")
    case "pulse" =>
      amplifier.pulse(pulseMag)
      nextStep("go away")
    case "go away" =>
      Talkative.getResponse(xyz, PiezoStage.MoveBy(Vec(0,0, -stepSize)))
      Finished
    case _ => throw new Exception("Unrecognised state command")
  }

  private def nextStep(name:String): ContinueQ = {
    state = name
    Continue
  }

}


object StepAndPulse {
  def apply(stepSize: Double, pulseMag: Double): StepAndPulse = new StepAndPulse(stepSize, pulseMag)
}