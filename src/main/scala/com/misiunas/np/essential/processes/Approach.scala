package com.misiunas.np.essential.processes

import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess.{Finished, ContinueQ, Continue}
import com.misiunas.np.essential.{ACDC, Amplifier, DeviceProcess}
import com.misiunas.np.hardware.stage.PiezoStage
import com.misiunas.np.hardware.stage.PiezoStage.MoveBy
import com.misiunas.np.tools.Talkative
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec

/**
 * # Coordinates capillary approach to the surface
 *
 * ToDo:
 *  - implement PID controller for the approach:
 *    handle overshoot
 *  - Probability buffer to see if that was a fluke or permanent change
 *
 * Created by kmisiunas on 15-09-04.
 */
class Approach private ( val baselineFn: Amplifier => ACDC,
                         val target: Double, // expressed in percent
                         val speed: Double  // step size of
                         ) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  log.info("Approach speed is: "+speed+" um/iteration")

  type Probability = Double

  lazy val baseline: ACDC = baselineFn( amplifier )

  override def init() = {
    baseline // compute baseline for the first time
    log.info("Approach baseline was set to: "+baseline)
    log.info("Cost function for baseline is: "+costFunction(baseline))
    // todo: add DC / ACDC mode read
  }

  /** function for approaching the sample */
  override def step(): ContinueQ = {
    // get new amplifier readings
    amplifier.updateTillNew() // locks
    // measure IV
    val x = amplifier.get
    // occasional reporting
    if(System.currentTimeMillis()/1000 % 2 == 0) log.info("Cost function for current position is: "+costFunction(x))
    // estimate probability of being in the wright place
    val p = alarmTrigger( x )
    // have we arrived?
    p match {
      case p if p == 0.0 =>
        // far away, do big steps
        Talkative.getResponse( xyz , MoveBy( Vec(0,0, speed) ) )
        Continue
      case p if p < 1.0 =>
        // smaller steps if we are close
        val step = 0.9*speed*(1-p)+0.1*speed
        Talkative.getResponse( xyz , MoveBy( Vec(0,0, step) ) )
        Continue
      case p if p == 1.0 =>
        // there but test if everything ok
        amplifier.wait(10)
        val meanX = amplifier.getMean(10)
        if( alarmTrigger(meanX) < 0.95 )  // still not there
          Continue
        else // otherwise - we have arrived
          Finished
    }
  }
  
  
  /** function for evaluating cost of dropping */
  def costFunction(y: ACDC): Double = {
    val x = normalise(y)
    // mean - std
    val kappa = 0.5
    val mean = (x.ac+x.dc)/2
    def pow2(x: Double) = x*x
    mean - kappa * math.sqrt( pow2(x.ac - mean) +  pow2(x.dc - mean) )
  }

  /** returns values in a range from 0 to 1.0 */
  def normalise(x: ACDC): ACDC = ACDC(ac = x.ac.abs /baseline.ac.abs, dc = x.dc.abs/baseline.dc.abs)

  /** what is the probability that target was reached? */
  def alarmTrigger(x: ACDC): Probability = {
    // simple linear approximation
    val current = costFunction( x )
    val baseline: Double = 1.0 //normalised ;) costFunction( normalise( this.baseline ) )
    if(current > baseline)
      0.0
    else if(current < target)
      1.0
    else
      1.0 - (current - target)/(baseline - target)
  }

}



object Approach {

  // # Init

  /** safe and automatic method */
  def auto(): Approach = {
    val R = ConfigFactory.load.getDouble("experiment.tipRadius")
    val fn: Amplifier => ACDC = a => {a.wait(10); a.getMean(10)}
    new Approach(
      baselineFn = fn,
      target = 0.85,
      speed = R/4
       // safe side
    )
  }

  def apply(target: Double, speed: Double): Approach = {
    val fn: Amplifier => ACDC = a => {a.wait(10); a.getMean(10)}
    new Approach(fn, target, speed )
  }


}