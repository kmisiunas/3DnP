package com.misiunas.np.hardware.adc.input

import java.net.Socket

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import com.typesafe.config.ConfigFactory

/**
 * # Manages data storage from ADC received from TCP
 *
 * Contains:
 *  - Data buffer
 *  - Logging feature
 *
 * Created by kmisiunas on 15-08-27.
 */
class IVDataActor extends Actor with ActorLogging {



  override def receive: Receive = ???


}

object IVDataActor {
  def props(): Props = Props(new IVDataActor() )
}