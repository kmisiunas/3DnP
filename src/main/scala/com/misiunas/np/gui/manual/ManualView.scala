package com.misiunas.np.gui.manual

import akka.actor.{Props, ActorRef, ActorLogging, Actor}

/**
 * # Manual control over the procedures
 *
 * Created by kmisiunas on 15-09-08.
 */
class ManualView (val processor: ActorRef,
                  val manualController: ManualController  ) extends Actor with ActorLogging {

  override def preStart() = {
    // ensure 'hack-glue' between javaFX and Akka is there
    manualController.setActor( self )
    // run regular updates
    //system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")
  }

  override def receive: Receive = {
    case p: com.misiunas.np.essential.Process[Any] =>
      log.info("Manual control sent a process for processor: "+p)
      processor ! p
    case _ => log.warning("Message not understood")
  }

}


object ManualView{

  def props(processor: ActorRef,manualController: ManualController  ): Props =
    Props( new ManualView(processor, manualController ) )

}