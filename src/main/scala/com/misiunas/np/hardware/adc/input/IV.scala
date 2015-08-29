package com.misiunas.np.hardware.adc.input

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}

/**
 * Created by kmisiunas on 15-08-27.
 */
class IV extends Actor with ActorLogging {


  override def receive: Receive = {
???
  }



}


object IV {
  def props(): Props = Props(new IV() )

  // # User Request



}