package com.misiunas.np.essential.processes

import scalax.io.{Codec, Seekable, Resource}
import breeze.stats._
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess._
import com.misiunas.np.essential.processes.minor.MeasureCurrentBaseline
import com.misiunas.np.tools.PIDController
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime


/** Process for growing a pillar of height dZ
  *
  * Monitor AC channel to keep target distance
  * At the same time keep applying a small voltage offset that glows the pillar
  *
  * Requires well adjusted PID controller
  *
  * Created by kmisiunas on 2016-08-14.
  */
class GrowPillar (dZ: Double) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)

  import collection.JavaConversions._

  val target = 0.85
  val safetyDz = 20.0

  var totalSteps: Int = 0
  val voltage = 100.0

  val maxSpeed: Double = 0.03

  val remeasureInterval: Double = 10*60

  val pid: PIDController = PIDController( // does this allow to update paramters?
    kp = ConfigFactory.load.getDouble("approach.pid.kp"),
    ki = ConfigFactory.load.getDouble("approach.pid.ki"),
    kd = ConfigFactory.load.getDouble("approach.pid.kd")
  )

  override def toString: String = "GrowPillar(step="+totalSteps+")"

  var file: Seekable = null

  override def onStart(): Unit = {
    file = Resource.fromFile("GrowPillar.csv")
    amplifier.setPulseVoltage(voltage)
  }


  private var status: String = "init"

  override def step(): StepResponse = status match {
    case "init" =>
      status = "approach"
      //amplifier.setVoltage( voltage ) // todo
      DeviceProcess.Continue

    case "approach" =>
      status = "pid"
      pid.reset()
      InjectProcess( Approach.manualOld(target, true, 10) )

    case "pid" =>
      amplifier.updateTillNew() // locks thread
      reportState()
      pidStep()

    case _ => Panic("Command not found")
  }

  override def onStop(): ProcessResults = {
    probe.moveBy( Vec(0,0,-safetyDz) )
    //amplifier.setVoltage( 0.0 ) // todo need this latter on for automation
    Success
  }



  def pidStep(): StepResponse = pid.iterate(amplifier.track , target) match {

    case _ if amplifier.trackTimeSinceBaselineMeasured.getMillis/1000 > remeasureInterval =>
      pid.reset()
      InjectProcess( MeasureCurrentBaseline() )

    case _ if mean( amplifier.trackList(5) ) > 1.05 => // something is off with baseline
      pid.reset()
      log.info("Measure track current is high at " + amplifier.track)
      InjectProcess( MeasureCurrentBaseline() )

    case _ if amplifier.track < 0.2 => // is DAC dead?
      pid.reset()
      log.error("Unexpected ADC value: "+amplifier.track)
      probe.moveByGuranteed( Vec(0,0, -10.0) )
      Finished

    case p: Double if p.abs > maxSpeed =>
      probe.moveBy( Vec(0,0, p.signum*maxSpeed*0.5) ) //todo
      Continue

    case p: Double =>
      probe.moveByGuranteed( Vec(0,0, p) ) //todo
      Continue
  }




  def reportState(): Unit = {
    val pos = probe.pos
    val line: String = DateTime.now().toString() + ", " +
      pos.x +", "+pos.y+", "+pos.z+", " +
      mean( amplifier.trackList(10) ) + ", " +
      stddev( amplifier.trackList(10) ) +", " +
      amplifier.trackBaselineCurrent + ", " +
      voltage
    try{
      file.append(line+"\n")
      log.warn(line)
    } catch {
      case _ => log.warn(line)
    }
  }

}

object GrowPillar {
  def apply(dZ: Double): GrowPillar = new GrowPillar(dZ)
}