package com.misiunas.np.gui.manual

import javafx.application.Platform

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.misiunas.np.essential.Processor.ProcessorDoNow
import com.misiunas.np.essential.{DeviceProcess, Processor}
import com.misiunas.np.hardware.stage.PiezoStage

import scala.concurrent.duration._


/**
 * # Manual control over the procedures
 *
 * Created by kmisiunas on 15-09-08.
 */
class ManualView (val processor: ActorRef,
                  val manualController: ManualController  ) extends Actor with ActorLogging {

  import context._
  implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }

  val updateFPS = 30.0


  override def preStart() = {
    // ensure 'hack-glue' between javaFX and Akka is there
    manualController.setActor( self )
    // run regular updates
    system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")
  }

  override def receive: Receive = {
    case "tick" =>
      // sent info requests
      processor ! Processor.StatusQ
      context.system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")

    case p: DeviceProcess => processor ! p
    case p: ProcessorDoNow => processor ! p

    case cmd: Processor.Command => processor ! cmd

    case status: Processor.Status =>
      Platform.runLater(() => manualController.setProcessorStatus(status) )

    case msg => log.warning("Message not understood: "+msg)
  }

}


object ManualView{

  def props(processor: ActorRef,manualController: ManualController  ): Props =
    Props( new ManualView(processor, manualController ) )

}