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
import org.joda.time.DateTime

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
                        val stepsToConfirm: Int,
                        val speed: Double
                         ) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  val toRemeasureBaseline = ConfigFactory.load.getBoolean("approach.baselineMeasurement.remeasure")
  val remeasureInterval = ConfigFactory.load.getDouble("approach.baselineMeasurement.interval")

  private var steps = 0
  private var pid: PIDController = null

  private var lastReport = (0.0, DateTime.now())


  override def toString: String = "Approach(steps="+steps+", target="+target+")"

  override def onStart() = {
    steps = 0
    log.info("Approach speed is: "+speed+" um/iteration")
    pid = PIDController(
      kp = ConfigFactory.load.getDouble("approach.pid.kp"),
      ki = ConfigFactory.load.getDouble("approach.pid.ki"),
      kd = ConfigFactory.load.getDouble("approach.pid.kd")
    )
    log.info("Approach PID is: "+pid )
    lastReport = (probe.posGlobal.z, DateTime.now())
  }

  /** function for approaching the sample */
  override def step(): StepResponse = {
    reportProgress()
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

//    case _ if amplifier.track < target => // just go
//      log.warn("Found surface by BRUTE approach. golbal Pos: " + probe.posGlobal )
//      Finished

    case _ if amplifier.track < 0.2 => // is DAC dead?
      pid.reset()
      log.error("Unexpected ADC value: "+amplifier.track)
      probe.moveByGuranteed( Vec(0,0, -10.0) )
      Finished

    case _ if amplifier.track > 0.98 => // just go
      pid.reset()
      probe.moveBy( Vec(0,0, speed) )
      Continue

    case p: Double if p.abs > speed =>
      probe.moveBy( Vec(0,0, p.signum*speed*0.5) ) //todo
      Continue

    case p: Double =>
      probe.moveBy( Vec(0,0, p) ) //todo
      Continue

    case unknown => Panic("Message nut expected"+unknown)

  }

  def haveArrived: Boolean = {
    val std: Double = 0.005 //stddev( amplifier.trackList(stepsToConfirm) )
    val m: Double = mean( amplifier.trackList(stepsToConfirm) )
    target - std < m && m < target + std
  }

  /** report  the progress */
  def reportProgress(): Unit = {
    steps = steps + 1
    if (steps % 100 == 0) {
      val newReport = (probe.posGlobal.z, DateTime.now())
      val speed: Double = (newReport._1 - lastReport._1) / (newReport._2.getMillis - lastReport._2.getMillis) * 1000
      log.info("Approaching surface: steps="+steps+" track="+amplifier.track+ " speed="+speed.toFloat+"[um/s]")
      lastReport = newReport
    }
  }



}



object Approach {

  def apply(target: Double): Approach =
    new Approach(
      target,
      1,
      speed =  ConfigFactory.load.getDouble("approach.speed")
    )

  def manualOld(target: Double, premeasureBaseline: Boolean, stepsToConfirm: Int): Approach = {
    new Approach(target, stepsToConfirm , speed =  ConfigFactory.load.getDouble("approach.speed"))
  }

  def manual(target: Double, stepsToConfirm: Int, speed: Double): Approach =
    new Approach(target, stepsToConfirm, speed)

}