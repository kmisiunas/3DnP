package com.misiunas.np.tools

/**
 *  # PID loop controller
 *
 *
 *  previous_error = 0
 *  integral = 0
    start:
      error = setpoint - measured_value
      integral = integral + error*dt
      derivative = (error - previous_error)/dt
      output = Kp*error + Ki*integral + Kd*derivative
      previous_error = error
      wait(dt)
      goto start
 *
 * Created by kmisiunas on 15-09-14.
 */
class PIDController(val kp: Double, val ki: Double, val kd: Double) extends Serializable {

  type StepSize = Double

  protected var previousError = 0.0
  protected var integral = 0.0

  def iterate(x: Double, aim: Double): StepSize = {
    val error = x - aim
    integral = integral + error
    val derivative = error - previousError
    val output = kp*error + ki*integral + kd*derivative
    previousError = error
    output
  }

  def reset() = {previousError=0.0; integral=0.0;}

  override def toString: String = "PIDController(kp="+kp+",ki="+ki+",kd="+kd+")"
}


object PIDController {

  def apply( kp: Double, ki: Double, kd: Double): PIDController =
    new PIDController( kp, ki , kd)
}