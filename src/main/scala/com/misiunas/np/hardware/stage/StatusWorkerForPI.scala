package com.misiunas.np.hardware.stage

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.communication.Communication
import com.misiunas.np.hardware.communication.Communication.{SerialAsk, SerialReply}
import com.misiunas.np.hardware.stage.PiezoStage.PiezoStatus
import org.joda.time.DateTime

import scala.concurrent.Await
import akka.pattern.{ ask, pipe }

/** # Universal status worker for PI devices
  *
  * Created by kmisiunas on 2016-07-23.
  */
class StatusWorkerForPI (val serial: ActorRef, axes: Int, interval: Int)
  extends Actor with ActorLogging {

  import context._

  import scala.concurrent.duration._

  override def preStart() = system.scheduler.scheduleOnce(200 millis, self, "tick")

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  def receive = {
    case "awake" => {log.debug("awake command initiated - no action taken")}
    case "tick" =>
      //log.debug("Received a 'tick'")
      // ask stage about this
      getPIStatus() match {
        case Some(status) => context.parent ! status
        case None => // not much to do
      }
      // send another periodic tick after the specified delay
      system.scheduler.scheduleOnce(interval millis, self, "tick")
  }

  /** request information about piezo */
  def getPIStatus(): Option[StatusPI] = {
    try {
      import Communication._
      implicit val timeout = Timeout(1 seconds)

      val positionFuture = serial ? SerialAsk("POS?", lines = axes)
      val positionReply = Await.result(positionFuture, timeout.duration).asInstanceOf[SerialReply].reply
      log.debug("raw POS? response: {}", positionReply)
      val position: Vector[Double] = interpretPositions(positionReply)
      val isMovingFuture = serial ? SerialAsk("" + 5.toChar)
      val isMovingReply = Await.result(isMovingFuture, timeout.duration).asInstanceOf[SerialReply].reply
      log.debug("PI response to \""+5.toChar+"\"  command: {}", isMovingReply)
      val isMoving = if (isMovingReply.head.trim == "0") false else true
      Some( StatusPI(DateTime.now, position, isMoving) )
    } catch {
      case e: Exception =>
        log.warning("Could not read status from PI: ", e.toString)
        None
    }
  }

  /** does the recognition of PI GSC command */
  def interpretPositions(msgs: List[String]): Vector[Double] = {
    def interpretLine(line:String): Option[Map[Int, Double]] = {
      """([1-3])=\-?((\d*\.\d*e[\+\-]\d+)|(\d*\.\d+))""".r.findFirstIn(line.trim) match {
        case None => None
        case Some(st) => Some( Map( st.take(1).toInt -> st.drop(2).toDouble ) )
      }
    }
    val pos: Map[Int, Double] = msgs.map(interpretLine)
      .filter(_.nonEmpty).map(_.get)
      .foldLeft(Map[Int, Double]())( (map, el) => map ++ el)
    log.debug("PI position was read to be: {}", pos)
    // handle incomplete messages here
    if (pos.size == axes)
      (1 to axes).map( pos ).toVector
    else
      throw new Exception("StatusWorkerForPI: got n="+pos.size+", but expected n="+axes)
  }


}

object StatusWorkerForPI {

  def props(serial: ActorRef, axes: Int, interval: Int): Props = Props(new StatusWorkerForPI( serial, axes, interval ) )

}

case class StatusPI(timestamp: DateTime,
                    pos: Vector[Double],
                    moving: Boolean) extends Serializable