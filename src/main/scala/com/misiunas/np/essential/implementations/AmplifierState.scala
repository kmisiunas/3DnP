package com.misiunas.np.essential.implementations

import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.hardware.adc.control.DAC.ElectrodeMode
import com.misiunas.np.tools.Talkative

/**
 * Created by kmisiunas on 15-09-17.
 */
abstract class AmplifierState extends AmplifierControl {


  def setElectrodeMode(mode: ElectrodeMode): Unit =
    Talkative.getResponse(dac,  DAC.SetMode(mode)) // wait for response because it is important


  /** sets device to zero DC current */
  def zeroCurrent(): Unit = {
    // ignore AC current
    // Go from current level based on IV readings, because this avoids overshoots
    // coarse current search
    //todo!
    // fine search

  }

}
