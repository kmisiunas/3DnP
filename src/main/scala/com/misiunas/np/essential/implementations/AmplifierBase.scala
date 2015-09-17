package com.misiunas.np.essential.implementations

import akka.actor.ActorRef

/**
 * # Underlying infrastructure for amplifier class
 *
 * Created by kmisiunas on 15-09-16.
 */
abstract class AmplifierBase {

  /** logger */
  protected val log = org.slf4j.LoggerFactory.getLogger(this.getClass)

  // # abstract methods

  val dac: ActorRef

  val iv: ActorRef


}
