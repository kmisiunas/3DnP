package com.misiunas.np.hardware.stage

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import breeze.numerics.log
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.communication.{Communication, CommunicationTCP}
import com.misiunas.np.hardware.stage.PiezoStage.PiezoStatus
import org.joda.time.DateTime

import scala.concurrent.Await
import scala.language.postfixOps

/** automatic query method to the Piezo stage
  *
  * Created by kmisiunas on 15-08-15.
  */
protected class StatusWorker (val serial: ActorRef) extends Actor with ActorLogging {

  import context._

  import scala.concurrent.duration._

  override def preStart() = system.scheduler.scheduleOnce(200 millis, self, "tick")

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  def receive = {
    case "awake" => {log.debug("awake zodziucommand initiated - no action taken")}
    case "tick" =>
      //log.debug("Received a 'tick'")
      // ask stage about this
      getPiezoStatus() match {
        case Some(status) => context.parent ! status
        case None => // not much to do
      }
      // send another periodic tick after the specified delay
      system.scheduler.scheduleOnce(100 millis, self, "tick")
  }

  /** request information about piezo */
  def getPiezoStatus(): Option[PiezoStatus] = {
    try {
      import Communication._
      implicit val timeout = Timeout(1 seconds)

      val positionFuture = serial ? SerialAsk("POS?", lines = 3)
      val positionReply = Await.result(positionFuture, timeout.duration).asInstanceOf[SerialReply].reply
      log.debug("raw POS? response: {}", positionReply)
      val position: Vec = interpretPositions(positionReply)

      val isMovingFuture = serial ? SerialAsk("" + 5.toChar)
      val isMovingReply = Await.result(isMovingFuture, timeout.duration).asInstanceOf[SerialReply].reply
      log.debug("Piezo response to \""+5.toChar+"\"  command: {}", isMovingReply)
      val isMoving = if (isMovingReply.head.trim == "0") false else true

      Some( PiezoStatus(DateTime.now, position, isMoving) )
    } catch {
      case e: Exception =>
        log.warning("Could not read status from piezo: ", e.toString)
        None
    }
  }

  /** does the recognition of PI GSC command */
  def interpretPositions(msgs: List[String]): Vec = {
    def interpretLine(line:String): Option[Map[Int, Double]] = {
      """([1-3])=\-?\d*\.\d*e[\+\-]\d+""".r.findFirstIn(line.trim) match {
        case None => None
        case Some(st) => Some( Map( st.take(1).toInt -> st.drop(2).toDouble ) )
      }
    }
    val pos = msgs.map(interpretLine)
      .filter(_.nonEmpty).map(_.get)
      .foldLeft(Map[Int, Double]())( (map, el) => map ++ el)
    log.debug("Piezo position was read to be: {}", pos)
    // handle incomplete messages here
    if( pos.contains(1) && pos.contains(2) && pos.contains(3) )
        Vec( List(1,2,3).map(x => pos(x) ) )
    else
      throw new Exception("TCP: part of the message was missing")
  }


}

object StatusWorker {
  def props(tcp: ActorRef): Props = Props(new StatusWorker( tcp ) )
}
