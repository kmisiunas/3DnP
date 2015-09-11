package com.misiunas.np.essential.implementations

import akka.actor.ActorRef
import com.misiunas.np.essential.{ACDC, Amplifier}
import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.hardware.adc.input.IV
import org.joda.time.DateTime

/**
 * Created by kmisiunas on 15-09-04.
 */
class VoltageMode ( override val dac: ActorRef,  override val iv: ActorRef) extends Amplifier(dac, iv) {


  override def get: ACDC = ACDC( ac = data.ac_I.head, dc =  data.dc_I.head )

  override def get(cycles: Int): Vector[ACDC] = {
    val ac =  data.ac_I.take(cycles)
    val dc =  data.dc_I.take(cycles)
    ac.zip(dc).map(zipped => ACDC(zipped._1, zipped._2))
  }

}
