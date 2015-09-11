package com.misiunas.np.hardware.adc.control

/**
 * Created by kmisiunas on 15-09-02.
 */
case class DACStatus ( val dc_v: Double,
                       val ac_v: Double,
                       val ac_frequency: Double,
                       val ac: Boolean,
                       val electrode_mode: ElectrodeMode
                       )


abstract class ElectrodeMode

case object ImagingElectrode extends ElectrodeMode
case object DepositionElectrode extends ElectrodeMode
