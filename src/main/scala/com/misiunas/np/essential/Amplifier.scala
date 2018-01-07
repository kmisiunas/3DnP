package com.misiunas.np.essential

import akka.actor.ActorRef
import com.misiunas.np.essential.implementations.{AmplifierState, VoltageMode}
import com.misiunas.np.tools.Wait
import org.joda.time.{DateTime, Duration, Interval}

import scala.annotation.tailrec
import scala.util.control.Breaks._

/**
 * # Generic class for accessing and controlling amplifier
 *
 * ## Model:
 *
 *  manual update() -> fix readings -> output parameters
 *
 * ## Design:
 *  - layer for algorithms to access the amplifier
 *  - generic measure output that works for current and voltage modes
 *  - stabilisation layer so that output appears to be stable
 *
 * ## ToDo
 *  - Safety, if IV or DAC is dead
 *  - model fitting
 *
 * Created by kmisiunas on 15-09-04.
 */
abstract class Amplifier (val dac: ActorRef, val iv: ActorRef) extends AmplifierState {

  // # Temporary place for normalised position parameter measure

  private var approachModeAC: Boolean = true
  private var baseline: ACDC = null
  private var baselineTimestamp: DateTime = DateTime.parse("1988-04-27T00:00") // just large time interval

  private def normalisedCurrent(acdc: ACDC): Double =
    if (approachModeAC){
      acdc.ac / trackBaseline.ac
    } else {
      acdc.dc / trackBaseline.dc
    }

  def track = normalisedCurrent( this.get )

  def trackList(i: Int): Vector[Double] = this.get(i).map(normalisedCurrent)

  def trackModeAC(ac: Boolean): Unit = {approachModeAC = ac}

  def trackBaseline: ACDC = baseline

  def trackBaselineCurrent: Double = if(approachModeAC) baseline.ac else baseline.dc

  def trackTimeSinceBaselineMeasured: Duration  = (new Interval(baselineTimestamp,DateTime.now())).toDuration

  /** Measured the baseline (it will lock!)
    * Dynamically load points until error is low enough  */
  def trackMeasureBaseline(): Boolean = {
//    import breeze.linalg._
//    import breeze.numerics._
//    import breeze.stats._
    this.await(10)
    baseline = this.getMean(10)
    baselineTimestamp = DateTime.now()
    true
  }


}

object Amplifier {

  def voltageMode(dac: ActorRef, iv: ActorRef): Amplifier = {
    val amp = new VoltageMode( dac, iv )
    Wait.stupid(1000) // silly implementation
    amp.update()
    amp
  }



}
