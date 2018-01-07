package com.misiunas.np.essential.processes

import akka.actor.SupervisorStrategy.Stop
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess.{Continue, StepResponse, Finished}
import com.misiunas.np.essential.{ACDC, Amplifier, DeviceProcess}
import com.misiunas.np.hardware.stage.PiezoStage
import com.misiunas.np.hardware.stage.PiezoStage.{MoveBy, PositionQ}
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
class ApproachOld private(val baselineFn: Amplifier => ACDC,
                          val target: Double, // expressed in percent
                          val speed: Double // step size of
                         ) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  log.info("Approach speed is: "+speed+" um/iteration")

  type Probability = Double


  // experimental method for aproaching the tip
  class Baseline {

    private var lastCheck: Long = 0

    private var mean: ACDC = measure()

    // get
    def apply(): ACDC = mean

    val toRemeasure = ConfigFactory.load.getBoolean("approach.baselineMeasurement.remeasure")
    val interval = ConfigFactory.load.getDouble("approach.baselineMeasurement.interval")
    val retreat = ConfigFactory.load.getDouble("approach.baselineMeasurement.retreat")
    val recover = ConfigFactory.load.getDouble("approach.baselineMeasurement.recover")

    def measure(): ACDC = {
      lastCheck = System.currentTimeMillis();
      mean = baselineFn(amplifier);
      mean
    }

    def timeToRemeasure(): Boolean = toRemeasure && lastCheck + interval*1000 >= System.currentTimeMillis()


  }

  lazy val baseline: Baseline = new Baseline()


  override def onStart() = {
    ConfigFactory.load.getBoolean("approach.baselineMeasurement")
    ConfigFactory.load.getDouble("approach.baselineMeasurementInterval")
    baseline // compute baseline for the first time
    log.info("Approach baseline was set to: "+baseline())
    log.info("Cost function for baseline is: "+ costFunction(baseline()))
    // todo: add DC / ACDC mode read
  }

  /** function for approaching the sample */
  override def step(): StepResponse = {
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
      case p if p == 0.0 && baseline.timeToRemeasure() =>
        // far away, time to remeasure
        Talkative.getResponse( xyz , MoveBy( Vec(0,0, -baseline.retreat) ) )
        baseline.measure()
        Talkative.getResponse( xyz , MoveBy( Vec(0,0, baseline.recover) ) )
        log.info("Remeasured the baseline: "+baseline())
        Continue
      case p if p == 1.0 =>
        // there but test if everything ok
        amplifier.await(10)
        val meanX = amplifier.getMean(10)
        if( alarmTrigger(meanX) < 0.95 )  // still not there
          Continue
        else // otherwise - we have arrived
          Finished
      case p if p < 1.0 =>
        // smaller steps if we are close
        val step = 0.9*speed*(1-p)+0.1*speed
        Talkative.getResponse( xyz , MoveBy( Vec(0,0, step) ) )
        Continue
      case p if Talkative.getXYZPosition( xyz ).z > 99.0 =>
        // far away, do big steps
        log.info("Failed to find surface on this approach")
        Finished
      case p if p == 0.0 =>
        // far away, do big steps
        Talkative.getResponse( xyz , MoveBy( Vec(0,0, speed) ) )
        Continue
    }
  }


  /** function for evaluating cost of dropping */
  def costFunctionAdvances(y: ACDC): Double = {
    val x = normalise(y)
    // mean - std
    val kappa = 0.5
    val mean = (x.ac+x.dc)/2
    def pow2(x: Double) = x*x
    mean - kappa * math.sqrt( pow2(x.ac - mean) +  pow2(x.dc - mean) )
  }

  /** function for evaluating cost of dropping */
  def costFunction(y: ACDC): Double = normalise(y).dc


  /** returns values in a range from 0 to 1.0 */
  def normalise(x: ACDC): ACDC = ACDC(ac = x.ac.abs /baseline().ac.abs, dc = x.dc.abs/baseline().dc.abs)

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



object ApproachOld {

  // # Init

  /** safe and automatic method */
  def auto(): ApproachOld = {
    val R = ConfigFactory.load.getDouble("experiment.tipRadius")
    val fn: Amplifier => ACDC = a => {a.await(10); a.getMean(10)}
    new ApproachOld(
      baselineFn = fn,
      target = 0.85,
      speed = R/4
       // safe side
    )
  }

  def apply(target: Double, speed: Double): ApproachOld = {
    val fn: Amplifier => ACDC = a => {a.await(10); a.getMean(10)}
    new ApproachOld(fn, target, speed )
  }


}