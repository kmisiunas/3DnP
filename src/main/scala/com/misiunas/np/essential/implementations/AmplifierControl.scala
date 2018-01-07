package com.misiunas.np.essential.implementations

import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.hardware.adc.control.DAC.Pulse
import com.misiunas.np.tools.Talkative

/**
 * Created by kmisiunas on 15-09-17.
 */
abstract class AmplifierControl extends AmplifierReaders {

  def pulse(magnitude: Double): Unit = {
    dac ! DAC.Pulse(magnitude)
    this.await(1)
  }

  def setPulseVoltage(v: Double): Unit = dac ! DAC.SetPulse_V(v)



}
