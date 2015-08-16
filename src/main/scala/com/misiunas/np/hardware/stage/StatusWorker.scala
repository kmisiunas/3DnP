package com.misiunas.np.hardware.stage

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import com.misiunas.geoscala.vectors.Vec
import org.joda.time.DateTime
import scala.concurrent.Await

import scala.language.postfixOps

/**
 * # automatic query method to the Piezo stage
 *
 * Created by kmisiunas on 15-08-15.
 */
protected class StatusWorker (val tcp: ActorRef) extends Actor {

  import context._

  import scala.concurrent.duration._

  override def preStart() = system.scheduler.scheduleOnce(500 millis, self, "tick")

  // override postRestart so we don't call preStart and schedule a new message
  override def postRestart(reason: Throwable) = {}

  def receive = {
    case "awake" => {}
    case "tick" =>
      // do something useful here
      context.parent ! getPiezoStatus()
      // send another periodic tick after the specified delay
      system.scheduler.scheduleOnce(100 millis, self, "tick")
  }

  /** request information about piezo */
  def getPiezoStatus(): PiezoStatus = {
    import TCPSimple._
    implicit val timeout = Timeout(1 seconds)

    val positionFuture = tcp ? TCPAsk("POS?")
    val positionReply = Await.result(positionFuture, timeout.duration).asInstanceOf[TCPReply].reply
    val position: Vec = interpretPositions(positionReply)

    val isMovingFuture = tcp ? TCPAsk("?")
    val isMovingReply = Await.result(isMovingFuture, timeout.duration).asInstanceOf[TCPReply].reply
    val isMoving = if(isMovingReply.head.trim == "0") false else true

    PiezoStatus(DateTime.now, position, isMoving)
  }

  /** does the recognition of PI GSC command */
  def interpretPositions(msgs: List[String]): Vec = {
    def interpretLine(line:String): Option[Map[Int, Double]] = {
      """([1-3])=\d*\.\d*e[+-]\d+""".r.findFirstIn(line.trim) match {
        case None => None
        case Some(st) => Some( Map( st.take(1).toInt -> st.drop(2).toDouble ) )
      }
    }
    println("=> Piezo pos raw: "+ msgs)
    val pos = msgs.map(interpretLine)
      .filter(_.nonEmpty).map(_.get)
      .foldLeft(Map[Int, Double]())( (map, el) => map ++ el)
    // return sorted results
    Vec( List(1,2,3).map(x => pos(x) ) )
  }


}

object StatusWorker {
  def props(tcp: ActorRef): Props = Props(new StatusWorker( tcp ) )
}
