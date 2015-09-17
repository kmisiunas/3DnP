package com.misiunas.np.essential.processes

import breeze.linalg.*
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess.{Continue, ContinueQ}
import com.misiunas.np.hardware.stage.PiezoStage
import com.misiunas.np.tools.{PIDController, Talkative}

import scala.annotation.tailrec

/**
 * # Function finds surface and keeps fixed distance to it
 *
 * Features:
 *  - periodically go up and check the baseline
 *
 *  Implementation is different from Approach process to test the best one
 *
 * Created by kmisiunas on 15-09-11.
 */
class KeepDistance (distanceFraction: Double,
                    approachSpeed: Double,
                    baselineCheckInterval: Double) extends DeviceProcess {


  var baseline: Double = 0.0
  var lastBaselineCheck: Long = 0

  val pid: PIDController = PIDController(
    kp = 5.0/1000.0*100/20, // for 1% deviation, take 5nm step
    ki = 1.0/1000.0*100/20, // guess value
    kd = 0.0  // sensitive to noise
  )

  /** method for keeping close to the surface
    * derivative action is sensitive to measurement noise, so avoid it
    */
  def step(): ContinueQ = {
    amplifier.updateTillNew()
    // if the current is more than 95% of baseline go to approach method
    if (normalise( amplifier.get.dc ) > 0.95 ){
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,approachSpeed) ))
      pid.reset()
      Continue
    } else if(System.currentTimeMillis() > lastBaselineCheck + baselineCheckInterval*1000) {
      // move away and measure the baseline
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,-10) ))
      baseline = readBaseline()
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,8) ))
      // leaves 2um to find with approach method
      pid.reset()
      lastBaselineCheck = System.currentTimeMillis()
      Continue
    } else {
      val dz = pid.iterate( normalise(amplifier.get.dc ), distanceFraction)
      val step = if(dz.abs <= approachSpeed) dz else {println("KeepDistance: PID step too large: "+dz); math.signum(dz)*approachSpeed}
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,step) ))
      Continue
    }
  }

  // # Implementation Methods

  /** prepare  */
  override def init() = {
    baseline = readBaseline()
    lastBaselineCheck = System.currentTimeMillis()
  }


  // # OLD - incorporated into other methods

  /** stupid approach method: approach until current dropped 5% */
  @tailrec
  final def approach(speed: Double): Unit = {
    amplifier.updateTillNew()
    if (normalise( amplifier.get.dc ) > 0.95){
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,speed) ))
      approach(speed)
    }
  }


  // # Helper methods

  def readBaseline(n: Int = 10): Double = {
    amplifier.wait(n)
    val mean = amplifier.getMean(n)
    mean.dc
  }

  def normalise(x: Double): Double = x.abs / baseline.abs

}


object KeepDistance {
  def apply(distanceFraction: Double, approachSpeed: Double, baselineCheckInterval: Double): KeepDistance =
    new KeepDistance(distanceFraction, approachSpeed, baselineCheckInterval)
}
