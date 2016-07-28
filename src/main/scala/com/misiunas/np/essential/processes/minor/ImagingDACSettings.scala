package com.misiunas.np.essential.processes.minor

import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.essential.DeviceProcess.{Finished, StepResponse}
import com.misiunas.np.hardware.adc.control.DAC

/**
 * Created by kmisiunas on 15-09-11.
 */
class ImagingDACSettings extends DeviceProcess {

  /** method to be implemented to determine a process */
  override def step(): StepResponse = {
    amplifier.dac ! DAC.SetMode(DAC.ImagingElectrode)
    amplifier.dac ! DAC.SetAC(false)
    amplifier.dac ! DAC.SetAC_V(100)
    amplifier.dac ! DAC.SetFrequency(100)
    amplifier.dac ! DAC.SetDC_V(-200)
    Finished
  }

}

object ImagingDACSettings{
  def apply(): ImagingDACSettings = new ImagingDACSettings()
}
