package com.misiunas.np.essential.processes

import com.misiunas.np.essential.DeviceProcess

/**
 * Created by kmisiunas on 15-09-11.
 */
class KeepDistance extends DeviceProcess[Unit] {
  /** method to be implemented to determine a process */
  override protected def process: Option[Unit] = ???
}


object KeepDistance {
  def apply(distanceFraction: Double, approachSpeed: Double): KeepDistance =
    ???
}
