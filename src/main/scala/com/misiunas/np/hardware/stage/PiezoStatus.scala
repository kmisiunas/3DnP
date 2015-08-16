package com.misiunas.np.hardware.stage

import com.misiunas.geoscala.vectors.Vec
import org.joda.time.DateTime

/**
 * # Class for storing status of a piezo stage
 *
 * Created by kmisiunas on 15-08-15.
 */
case class PiezoStatus ( lastUpdate: DateTime,
                         pos: Vec,
                         moving: Boolean )
