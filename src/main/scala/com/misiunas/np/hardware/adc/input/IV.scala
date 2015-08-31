package com.misiunas.np.hardware.adc.input

import java.net.Socket

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import com.misiunas.np.hardware.adc.input.IV.{IVData, IVDataRaw, GetRaw, Get}
import com.misiunas.np.hardware.adc.input.IVDataActor.RawDataContainer
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

/**
 * Created by kmisiunas on 15-08-27.
 */
class IV extends Actor with ActorLogging {

  val bufferSize: Int = ConfigFactory.load.getInt("adc.bufferSize")

  val ivDataSource = context.actorOf(IVDataActor.props(), "parser")

  var dataRaw: IVDataRaw = IVDataRaw( DateTime.now(), 0, Vector[Double](), Vector[Double]())

  var data: IVData = IVData( DateTime.now(), 0 , Vector[Double](), Vector[Double](), Vector[Double](), Vector[Double](), Vector[Double]() )

  override def receive: Receive = {
    case Get(n) => sender ! data
    case GetRaw(tn) => sender ! dataRaw
    case data: RawDataContainer if sender == ivDataSource  =>  updateDatabase(data)
  }

  def updateDatabase(in: RawDataContainer): Unit = {
    dataRaw = IVDataRaw( in.t, in.dt / in.raw_I.length, in.raw_I, in.raw_V )
    // make sure data is consistent with previous data
    checkDataConsistency(in)
    // buffer filled?
    data = IVData( //todo: is this too slow?
      in.t,
      in.dt,
      in.ac_I  +: data.ac_I.take(bufferSize-1),
      in.ac_V  +: data.ac_V.take(bufferSize-1),
      in.dc_I  +: data.dc_I.take(bufferSize-1),
      in.dc_V  +: data.dc_V.take(bufferSize-1),
      in.phase +: data.phase.take(bufferSize-1)
    )
  }

  /** log problem,s with incoming data */
  def checkDataConsistency(in: RawDataContainer): Unit = {
    import com.github.nscala_time.time.Imports._
    //if ( (in.t to data.t).millis > 1000 )
    //  log.warning("there was no IV update for more than a second")
    if (in.dt != data.dt)
      log.warning("Change in sampling rate from "+in.dt+" to "+data.dt)
  }

}


object IV {
  def props(): Props = Props(new IV() )

  // # User Request

  /** returns broad sweep of IV characteristics over n iterations*/
  case class Get(val n: Int = 1)

  /** returns detailed IV trace for specified time interval */
  case class GetRaw(val tn: Int = 0)

  case class IVDataRaw(val t: DateTime, val dt: Double, val i: Vector[Double], val v: Vector[Double])

  case class IVData( val t: DateTime, val dt: Double,
                     val ac_I: Vector[Double], val ac_V: Vector[Double],
                     val dc_I: Vector[Double], val dc_V: Vector[Double],
                     val phase: Vector[Double] )


}