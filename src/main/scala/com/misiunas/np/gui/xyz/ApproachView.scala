package com.misiunas.np.gui.xyz

import javafx.application.Platform

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.approach.ApproachStage.ApproachStageStatus
import com.misiunas.np.hardware.stage.PiezoStage
import com.misiunas.np.hardware.stage.PiezoStage.PiezoStatus

import scala.concurrent.duration._


/**
  * Created by kmisiunas on 2016-07-21.
  */
class ApproachView (val approachStage: ActorRef,
                    val viewApproach: ApproachController  ) extends Actor with ActorLogging {

  import context._
  implicit def funToRunnable(fun: () => Unit) = new Runnable() { def run() = fun() }

  val updateFPS = 30.0

  override def preStart() = {
    // ensure 'hack-glue' between javaFX and Akka is there
    viewApproach.setActor( self )
    // run regular updates
    system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")
  }




  override def receive: Receive = {
    case "tick" =>
      // sent info requests
      approachStage ! PiezoStage.StatusQ
      approachStage ! PiezoStage.PositionQ
      context.system.scheduler.scheduleOnce(1000/updateFPS millis, self, "tick")

    case ps: ApproachStageStatus =>
      // update the controller
      Platform.runLater(() => {
        viewApproach.setZPos(ps.pos.z)
      })

    case v: Vec if sender == approachStage =>
      Platform.runLater(() => {
        viewApproach.setFieldPos(v) // todo
      })

    case move: PiezoStage.Move => approachStage ! move
    case move: PiezoStage.MoveBy => approachStage ! move
  }



}

object ApproachView {
  def props( approachStage: ActorRef,
             viewApproach: ApproachController
           ): Props =
    Props( new ApproachView(approachStage, viewApproach) )

}
