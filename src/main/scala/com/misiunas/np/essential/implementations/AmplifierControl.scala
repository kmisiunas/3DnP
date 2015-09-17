package com.misiunas.np.essential.implementations

import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.tools.Talkative

/**
 * Created by kmisiunas on 15-09-17.
 */
abstract class AmplifierControl extends AmplifierReaders {

  def pulseAndWait(magnitude: Double):Unit =
    Talkative.getResponse(dac, DAC.Pulse(magnitude))

}
