package com.misiunas.np.essential.processes

import breeze.linalg.*
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
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
                    baselineCheckInterval: Double) extends DeviceProcess[Unit] {


  var baseline: Double = 0.0
  var lastBaselineCheck: Long = 0

  val pid: PIDController = PIDController(
    kp = 5.0/1000.0*100, // for 1% deviation, take 5nm step
    ki = 1.0/1000.0*100, // guess value
    kd = 0.0  // sensitive to noise
  )

  override protected def process: Option[Unit] = {
    init()
    pidController()
    return None
  }

  // # Implementation Methods

  /** prepare  */
  def init() = {
    baseline = readBaseline()
    lastBaselineCheck = System.currentTimeMillis()
  }

  /** stupid approach method: approach until current dropped 5% */
  @tailrec
  final def approach(speed: Double): Unit = {
    amplifier.updateTillNew()
    if (normalise( amplifier.get.dc ) > 0.95){
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,speed) ))
      approach(speed)
    }
  }

  /** method for keeping close to the surface
    * derivative action is sensitive to measurement noise, so avoid it
    */
  @tailrec
  final def pidController(): Unit = {
    amplifier.updateTillNew()
    // if the current is more than 95% of baseline go to approach method
    if (normalise( amplifier.get.dc ) > 0.95 ){
      approach( approachSpeed )
      pid.reset()
      pidController()
    } else if(System.currentTimeMillis() > lastBaselineCheck + baselineCheckInterval*1000) {
      // move away and measure the baseline
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,-10) ))
      baseline = readBaseline()
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,8) ))
      // leaves 2um to find with approach method
      pid.reset()
      lastBaselineCheck = System.currentTimeMillis()
      pidController()
    } else {
      val dz = - pid.iterate( normalise(amplifier.get.dc ), distanceFraction)
      val step = if(dz.abs <= 0.1) dz else {println("KeepDistance: PID step too large: "+dz); math.signum(dz)*0.1}
      Talkative.getResponse(xyz, PiezoStage.MoveBy( Vec(0,0,step) ))
      pidController()
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
