package com.misiunas.np

import akka.actor.ActorSystem
import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.hardware.adc.input.IV
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * # Entry class for 3DnP
 *
 *
 * Created by kmisiunas on 15-09-04.
 */
class NanoPrinter {

  val system = ActorSystem("3DnP")

  val xyz = system.actorOf(PiezoStage.props(), "piezo")

  val iv = system.actorOf(IV.props(), "adc")

  val control = system.actorOf(DAC.props(), "dac")






}
