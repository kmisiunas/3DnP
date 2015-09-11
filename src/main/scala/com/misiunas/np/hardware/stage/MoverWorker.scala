package com.misiunas.np.hardware.stage

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.TCPSimple
import com.misiunas.np.hardware.stage.PiezoStage.{Move, Stop}
import TCPSimple._
import com.misiunas.np.tools.Wait
import com.typesafe.config.ConfigFactory

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._


/**
 * Executes movement commands one by one
 *
 * Created by kmisiunas on 15-08-02.
 */
protected class MoverWorker (val tcp: ActorRef) extends Actor with ActorLogging {

  import collection.JavaConversions._

  val min: Vec = Vec( ConfigFactory.load.getDoubleList("piezo.minPosition").toList.map(d => d.toDouble) )
  val max: Vec = Vec( ConfigFactory.load.getDoubleList("piezo.maxPosition").toList.map(d => d.toDouble) )


  override def receive: Receive = {
    case "awake" => {}
    case Move(r) =>
      if (isWithinRange(r))
        tcp ! TCPTell("MOV 1 " + r.x.toFloat + " 2 "+ r.y.toFloat + " 3 " + r.z.toFloat  )
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
    val isMovingFuture = tcp ? TCPAsk("" + 5.toChar)
    val isMovingReply = Await.result(isMovingFuture, timeout.duration).asInstanceOf[TCPReply].reply
    val isMoving = if (isMovingReply.head.trim == "0") false else true
    if(isMoving) waitUntilLastJobFinished()
  }

  def isWithinRange(r: Vec): Boolean =
    (r.x >= min.x && r.x <= max.x) && (r.y >= min.y && r.y <= max.y) && (r.z >= min.z && r.z <= max.z)


}

object MoverWorker{
  def props(tcp: ActorRef): Props = Props(new MoverWorker( tcp ) )
}
