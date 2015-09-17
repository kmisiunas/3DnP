package com.misiunas.np.essential

import akka.actor.ActorRef
import com.misiunas.np.essential.implementations.{AmplifierState, AmplifierReaders, VoltageMode}
import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.hardware.adc.input.IV
import com.misiunas.np.hardware.adc.input.IV.IVData
import com.misiunas.np.tools.{Wait, Talkative}
import org.joda.time.DateTime
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



}

object Amplifier {

  def voltageMode(dac: ActorRef, iv: ActorRef): Amplifier = {
    val amp = new VoltageMode( dac, iv )
    Wait.stupid(1000) // silly implementation
    amp.update()
    amp
  }

}
