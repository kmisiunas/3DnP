package com.misiunas.np.gui.manual

import javafx.application.Platform

import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import com.misiunas.np.essential.{DeviceProcess, Processor}

/**
 * # Manual control over the procedures
 *
 * Created by kmisiunas on 15-09-08.
 */
class ManualView (val processor: ActorRef,
                  val manualController: ManualController  ) extends Actor with ActorLogging {

  import context._
  implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }

  override def preStart() = {
    // ensure 'hack-glue' between javaFX and Akka is there
    manualController.setActor( self )
  }

  override def receive: Receive = {
    case p: DeviceProcess => processor ! p
    case Processor.Kill => processor ! Processor.Kill
    case Processor.Status(working) =>
      Platform.runLater(() => manualController.setProcessorStatus(working) )
    case msg => log.warning("Message not understood: "+msg)
  }

}


object ManualView{

  def props(processor: ActorRef,manualController: ManualController  ): Props =
    Props( new ManualView(processor, manualController ) )

}