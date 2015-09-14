package com.misiunas.np.gui.xyz

import javafx.application.Platform

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.stage.PiezoStage.PiezoStatus
import com.misiunas.np.hardware.stage.{PiezoStage}

import scala.concurrent.duration._


/**
 * Created by kmisiunas on 15-09-01.
 */
protected class XYZView (val piezo: ActorRef,
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
      piezo ! PiezoStage.PositionQ
      context.system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")

    case ps: PiezoStatus =>
      // update the controller
      Platform.runLater(() => {

        viewXYZ.setXPos(ps.pos.x)
        viewXYZ.setYPos(ps.pos.y)
        viewXYZ.setZPos(ps.pos.z)
      })

    case v: Vec if sender == piezo =>
      Platform.runLater(() => {
        viewXYZ.setFieldPos(v)
      })

    case move: PiezoStage.Move => piezo ! move

  }



}

object XYZView {
  def props(
             piezo: ActorRef,
             viewXYZ: XYZController   ): Props =
    Props( new XYZView(piezo, viewXYZ) )


}
