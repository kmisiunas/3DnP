package com.misiunas.np.hardware.stage

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.communication.Communication.{SerialAsk, SerialReply, SerialTell}
import com.misiunas.np.hardware.communication.CommunicationTCP
import com.misiunas.np.hardware.stage.PiezoStage.{Move, Stop}
import com.misiunas.np.tools.Wait
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._


/** Executes movement commands one at the time
  *
  * Warning - the axes for the materials are based on physical space, not representation
  *
  * Created by kmisiunas on 15-08-02.
 */
protected class MoveWorker(val serial: ActorRef, min: Vec, max: Vec) extends Actor with ActorLogging {


  override def receive: Receive = {
    case "awake" => {}
    case Move(r) =>
      if (isWithinRange(r))
        serial ! SerialTell( formulateMoveCommand(r) )
      else {  // todo fix this
        sender ! "Error: position (" + r.x + ", " + r.y + ", " + r.z + ") is outside the bounds. skipping."
        context.parent ! "Reset Position"
        log.error("Error: position (" + r.x + ", " + r.y + ", " + r.z + ") is outside the bounds. skipping.")
      }
      waitUntilLastJobFinished()
      sender ! "ok"
    // need to handle TCP failures?
  }

  /** waits until last job is finished */
  @tailrec
  private def waitUntilLastJobFinished(): Unit = {
    implicit val timeout = Timeout(1 seconds)
    val isMovingFuture = serial ? SerialAsk("" + 5.toChar)
    val isMovingReply = Await.result(isMovingFuture, timeout.duration).asInstanceOf[SerialReply].reply
    val isMoving = if (isMovingReply.head.trim == "0") false else true
    if(isMoving) waitUntilLastJobFinished()
  }

  def formulateMoveCommand(r: Vec): String = "MOV" + (
    if(min.x < max.x) {" 1 " + r.x.toFloat } else {""} ) + (
    if(min.y < max.y) {" 2 " + r.y.toFloat } else {""} ) + (
    if(min.z < max.z) {" 3 " + r.z.toFloat } else {""} )

  def isWithinRange(r: Vec): Boolean =
    (r.x >= min.x && r.x <= max.x) && (r.y >= min.y && r.y <= max.y) && (r.z >= min.z && r.z <= max.z)


}

object MoveWorker{
  def props(serial: ActorRef, min: Vec, max: Vec): Props = Props(new MoveWorker( serial, min , max ) )
}
