package com.misiunas.np.essential.processes.minor

import com.misiunas.np.essential.DeviceProcess
import com.misiunas.np.hardware.adc.control.DAC

/**
 * Created by kmisiunas on 15-09-11.
 */
class ImagingDACSettings extends DeviceProcess[Unit] {

  /** method to be implemented to determine a process */
  override protected def process: Option[Unit] = {
    amplifier.dac ! DAC.SetMode(DAC.ImagingElectrode)
    amplifier.dac ! DAC.SetAC(false)
    amplifier.dac ! DAC.SetAC_V(100)
    amplifier.dac ! DAC.SetFrequency(100)
    amplifier.dac ! DAC.SetDC_V(-200)
    return None
  }

}

object ImagingDACSettings{
  def apply(): ImagingDACSettings = new ImagingDACSettings()
}
