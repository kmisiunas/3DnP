package com.misiunas.np.essential.processes.minor

import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess.{Continue, Finished, Panic, StepResponse}
import com.typesafe.config.ConfigFactory

/** # Measures the baseline current for calibration
  *
  * For this the probe moves away, measures, and then recovers SOME of the initial distance
  *
  * Created by kmisiunas on 2016-07-27.
  */
class MeasureCurrentBaseline (fullRecovery: Boolean) extends DeviceProcess {

  val log = org.slf4j.LoggerFactory.getLogger(getClass.getName)


  val retreat = ConfigFactory.load.getDouble("approach.baselineMeasurement.retreat")
  val recover = if(fullRecovery) retreat else
    ConfigFactory.load.getDouble("approach.baselineMeasurement.recover")

  override def onStart(): Unit = {status = "retreat"}

  override def toString: String = "MeasureCurrentBaseline(status="+status+")"

  private var status: String = "retreat"

  /** steps Must perform small actions where the process can be broke in between */
  override def step(): StepResponse = status match {
    case "retreat" if probe.canMoveBy( Vec(0,0,-retreat) ) =>
      probe.moveBy( Vec(0,0,-retreat) )
      status = "measure"
      Continue
    case "retreat" => // could not move with piezo
      status = "measure"
      probe.moveApproachStageBy(Vec(0,0,-retreat))
      Continue
    case "measure" =>
      amplifier.trackMeasureBaseline()
      log.info("New baseline at "+amplifier.trackBaseline)
      status = "recover"
      Continue
    case "recover" =>
      probe.moveBy( Vec(0,0, recover) )
      Finished
  }
}

object MeasureCurrentBaseline {
  def apply(fullRecovery: Boolean = false): MeasureCurrentBaseline = new MeasureCurrentBaseline(fullRecovery)
}
