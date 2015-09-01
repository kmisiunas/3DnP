package com.misiunas.np.gui

import javafx.application.Platform

import akka.actor.{ActorLogging, ActorRef, Props, Actor}
import akka.actor.Actor.Receive
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.gui.xyz.XYZController
import com.misiunas.np.hardware.stage.{PiezoStatus, PiezoStage}
import scala.concurrent.duration._


/**
 * Created by kmisiunas on 15-09-01.
 */
protected class GuiActor (val piezo: ActorRef,
                          val viewXYZ: XYZController  ) extends Actor with ActorLogging {

  import context._
  implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }

  val updateFPS = 30.0

  override def preStart() = {
    // ensure 'hack-glue' between javaFX and Akka is there
    viewXYZ.setActor( self )

    // run regular updates
    system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")
  }




  override def receive: Receive = {
    case "tick" =>
      // sent info requests
      piezo ! PiezoStage.StatusQ
      context.system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")

    case ps: PiezoStatus =>
      // update the controller
      Platform.runLater(() => {
        viewXYZ.setXPos(ps.pos.x)
        viewXYZ.setYPos(ps.pos.y)
        viewXYZ.setZPos(ps.pos.z)
      })

    case move: PiezoStage.Move => piezo ! move

  }



}

object GuiActor {
  def props(
             piezo: ActorRef,
             viewXYZ: XYZController   ): Props =
    Props( new GuiActor(piezo, viewXYZ) )


}
