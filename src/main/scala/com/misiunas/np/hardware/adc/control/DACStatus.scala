package com.misiunas.np.hardware.adc.control

import com.misiunas.np.hardware.adc.control.DAC.ElectrodeMode

/**
 * Created by kmisiunas on 15-09-02.
 */
case class DACStatus ( val dc_v: Double,
                       val ac_v: Double,
                       val ac_frequency: Double,
                       val ac: Boolean,
                       val electrode_mode: DAC.ElectrodeMode
                       )


