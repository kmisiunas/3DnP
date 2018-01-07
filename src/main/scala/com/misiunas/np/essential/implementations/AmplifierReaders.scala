package com.misiunas.np.essential.implementations

import akka.actor.ActorRef
import com.misiunas.np.essential.ACDC
import com.misiunas.np.hardware.adc.input.IV
import com.misiunas.np.hardware.adc.input.IV.IVData
import com.misiunas.np.tools.Talkative
import org.joda.time.DateTime

import scala.annotation.tailrec
import scala.util.control.Breaks._

/**
 * Created by kmisiunas on 15-09-16.
 */
abstract class AmplifierReaders extends AmplifierBase {


  // # Updater model

  def update(): Unit = {
    rawData = Talkative.getResponse(iv, IV.Get() ).asInstanceOf[IVData]
  }

  /** data container for latest raw IV reading */
  private var rawData: IVData = null

  private var lastCheckCycle: Int = -1

  /** locks thread until new IV measurement comes in */
  @tailrec
  final def updateTillNew(): Unit = {
    if(cycle == lastCheckCycle) {
      update()
      updateTillNew()
    } else {
      lastCheckCycle = cycle
    }
  }

  /** raw data access */
  def data: IVData = rawData



  // # main universal access methods

  def get: ACDC

  def get(cycles: Int): Vector[ACDC]

  def getMean(cycles: Int): ACDC = {
    val list = get(cycles)
    val ac = list.map(_.ac)
    val dc = list.map(_.dc)
    ACDC( ac.sum / ac.length , dc.sum / dc.length)
  }


  // # Time management methods

  def timeStamp: DateTime = data.t

  def cycle: Int = data.cycle

  /** locks the operations until 'cycles' number of new readings have been received */
  def await(cycles: Int): Unit = {
    val start = cycle
    val startT = System.currentTimeMillis()
    while( start + cycles >= cycle){ // hack of a method
      update()
      if (System.currentTimeMillis() - startT > 5000) {
        log.warn("Timeout while waiting for " + cycles + " new cycles from IV")
        break
      }
    }
  }

}
