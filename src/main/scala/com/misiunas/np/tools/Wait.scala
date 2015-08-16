package com.misiunas.np.tools

/**
 * Created by kmisiunas on 15-08-16.
 */
object Wait {

  def stupid(ms: Int) = {
    val t0 = System.currentTimeMillis()
    while (t0 + ms >= System.currentTimeMillis()){}
  }

}
