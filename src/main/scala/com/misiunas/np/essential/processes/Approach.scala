package com.misiunas.np.essential.processes

import java.nio.file.{Paths, StandardOpenOption}

import akka.actor.SupervisorStrategy.Stop
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess._
import com.misiunas.np.essential.processes.minor.MeasureCurrentBaseline
import com.misiunas.np.essential.{ACDC, Amplifier, DeviceProcess}
import com.misiunas.np.hardware.stage.PiezoStage
import com.misiunas.np.hardware.stage.PiezoStage.{MoveBy, PositionQ}
import com.misiunas.np.tools.{PIDController, Talkative}
import com.typesafe.config.ConfigFactory


import breeze.stats._

/**
  * # Coordinates capillary approach to the surface
  *
  * ToDo:
  *  - implement PID controller for the approach:
  *    handle overshoot
  *  - Probability buffer to see if that was a fluke or permanent change
  *  - speed loaded from global settings? and auto updated?
  *
  * Created by kmisiunas on 15-09-04.
  */
class Approach private( val target: Double, // expressed in percent,
                        val premeasureBaseline: Boolean,
                        val stepsToConfirm: Int
                         ) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  val toRemeasureBaseline = ConfigFactory.load.getBoolean("approach.baselineMeasurement.remeasure")
  val remeasureInterval = ConfigFactory.load.getDouble("approach.baselineMeasurement.interval")

  val speed: Double = ConfigFactory.load.getDouble("approach.speed")

  private var steps = 0
  private var pid: PIDController = null

  override def toString: String = "Approach(steps="+steps+")"

  override def initialise() = {
    steps = 0
    log.info("Approach speed is: "+speed+" um/iteration")
    pid = PIDController(
      kp = ConfigFactory.load.getDouble("approach.pid.kp"),
      ki = ConfigFactory.load.getDouble("approach.pid.ki"),
      kd = ConfigFactory.load.getDouble("approach.pid.kd")
    )
    log.info("Approach PID is: "+pid )
    if (premeasureBaseline) amplifier.trackMeasureBaseline()
  }

  /** function for approaching the sample */
  override def step(): StepResponse = {
    if(steps % 100 == 0) log.info("Approaching surface: steps="+steps+" current track="+amplifier.track)
    steps = steps + 1
    amplifier.updateTillNew() // locks thread
    getCloser()
  }




  def getCloser(): StepResponse = pid.iterate(amplifier.track , target) match {

    case _ if toRemeasureBaseline && amplifier.trackTimeSinceBaselineMeasured.getMillis/1000 > remeasureInterval =>
      pid.reset()
      InjectProcess( MeasureCurrentBaseline() )

    case _ if mean( amplifier.trackList(5) ) > 1.05 => // something is off with baseline
      pid.reset()
      log.info("Measure track current is high at " + amplifier.track)
      InjectProcess( MeasureCurrentBaseline() )

    case _ if haveArrived =>
      log.info("Found surface at " + probe.posGlobal )
      Finished

    case  _ if  !probe.canMoveBy( Vec(0,0,speed) ) => // move Approach stage
      log.info("Moving PiezoStage down and bringing ApproachStage closer")
      probe.move( probe.pos.copy(z = 0.0) )
      probe.moveApproachStageBy(
        Vec(0,0, ConfigFactory.load.getDouble("approach.approachStageRecovery")) )
      Continue

    case p: Double if p.abs > 1 =>
      probe.moveBy( Vec(0,0, p.signum*speed) )
      Continue

    case p: Double =>
      probe.moveBy( Vec(0,0, p*speed) )
      Continue

    case unknown => Panic("Message nut expected"+unknown)

  }

  def haveArrived: Boolean = {
    val std: Double = stddev( amplifier.trackList(stepsToConfirm) )
    val m: Double = mean( amplifier.trackList(stepsToConfirm) )
    target - std < m && m < target + std
  }





}



object Approach {

  def apply(target: Double): Approach = { new Approach(target, true, 10 ) }

  def manual(target: Double, premeasureBaseline: Boolean, stepsToConfirm: Int): Approach = {
    new Approach(target, premeasureBaseline, stepsToConfirm )
  }


}