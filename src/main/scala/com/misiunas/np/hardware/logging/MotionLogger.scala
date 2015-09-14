package com.misiunas.np.hardware.logging

import java.io.{Writer, BufferedWriter, OutputStreamWriter, FileOutputStream}

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.adc.input.IV
import com.misiunas.np.hardware.stage.PiezoStage
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

/**
 * # Automatically logs every motion piezo stage is making
 *
 *
 * Created by kmisiunas on 15-09-14.
 */
class MotionLogger (val iv: ActorRef, val xyz: ActorRef) extends Actor with ActorLogging {

  var writer: Writer = null



  // # Kay actor methods

  override def preStart() = {
    // open file stream
    val file = ConfigFactory.load()
      .getString("logging.logPiezoFile")
      .replace("*DateTime*", DateTime.now.toString("YYYY-MM-dd HH:mm"))
    writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "utf-8"))
    writer.write("DateTime , X , Y , Z , realX , realY , realZ , DC_V , DC_I , AC_V , AC_I, dt \n")
    log.info("Opened motion logger to file: "+file)
  }

  override def receive: Receive = {
    case MotionLogger.Log if ConfigFactory.load().getBoolean("logging.logPiezo") =>
      // trigger logging procedure
      // if new log was finished before old data came in - just dump what ever was there already
      if (logOpen()) saveLine()
      // send out info requests
      xyz ! PiezoStage.StatusQ
      xyz ! PiezoStage.PositionQ
      iv ! IV.Get
      time = Some( DateTime.now() )
    case data: IV.IVData =>
      ivData = Some(data)
      if(isLogComplete()) saveLine()
    case ps: PiezoStage.PiezoStatus =>
      realPos = Some(ps.pos)
      if(isLogComplete()) saveLine()
    case v: Vec if sender == xyz =>
      pos = Some(v)
      if(isLogComplete()) saveLine()
  }

  override def postStop() = {
    writer.close()
  }

  // # Log helper methods

  def logOpen(): Boolean =
    pos.isDefined || realPos.isDefined || ivData.isDefined

  def isLogComplete(): Boolean =
    pos.isDefined && realPos.isDefined && ivData.isDefined

  /** saves all the reads to the file */
  def saveLine(): Unit = {
    // format the answer
    val line = "" +
      (time match {
        case Some(t) => t.toString
        case None => " - "
      }) + " , " +
      (pos match {
        case Some(v) => v.x +" , "+v.y+" , "+v.z
        case None => " ,  , "
      }) + " , " +
      (realPos match {
        case Some(v) => v.x +" , "+v.y+" , "+v.z
        case None => " ,  , "
      }) + " , " +
    (ivData match {
      case Some(iv) => iv.dc_V + " , "+ iv.dc_I + " , " + iv.ac_V + " , "+ iv.ac_I + " , " + iv.dt
      case None => " , , , , "
    }) + "\n"
    // write it
    writer.write(line)
    // clear buffer
    time = None
    pos = None
    realPos = None
    ivData = None
  }

  // # Variables for storing intermediate results

  var time: Option[DateTime] = None
  var pos: Option[Vec] = None
  var realPos: Option[Vec] = None
  var ivData: Option[IV.IVData] = None

}

object MotionLogger{

  def props(iv: ActorRef, xyz: ActorRef): Props = Props( new MotionLogger(iv, xyz) )

  /** request to log something */
  case object Log
}