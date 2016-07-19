package com.misiunas.np.hardware.adc.input

import java.net.Socket

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorLogging, Actor}
import com.misiunas.np.hardware.adc.input.IVDataActor.RawDataContainer
import com.misiunas.np.hardware.stage.MoveWorker
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import play.api.libs.json
import play.api.libs.json._

/**
 * # Manages data storage from ADC received from TCP
 *
 * Contains:
 *  - Data buffer
 *  - Logging feature
 *
 * Assumptions:
 *  - data is incoming sequentially (log warning otherwise)
 *
 * Created by kmisiunas on 15-08-27.
 */
class IVDataActor extends Actor with ActorLogging {

  // Data Contained

  private var lastN: Int = -1

  // Actor business

  /** TCP communication actor */
  private val tcp = context.actorOf(TCPStreamReader.props(), "tcp")

  override def receive: Receive = {
    case in: String if sender() == tcp =>
      context.parent ! precessIncomingData(in)
    case s: String => log.warning("Incoming unexpected string from " + sender)
      // also send it to data logger
  }

  // Data management

  /** parses incoming data stream */
  def precessIncomingData(in: String): RawDataContainer = {
    log.debug("Received a JSON string that is "+in.length+" chars long")
    try {
      val json: JsValue = Json.parse(in)
      // JSON structure must be synchronised with LABVIEW
      val n: Int = (json \ "LoopId").as[Int]
      val t = DateTime.parse( (json \ "Timestamp").as[String] )
      val ac_I = (json \ "I_AC").as[Double]
      val ac_V = (json \ "U_AC").as[Double]
      val dc_I = (json \ "I_DC").as[Double]
      val dc_V = (json \ "U_DC").as[Double]
      val phase = (json \ "Phase").as[Double]
      val dt = (json \ "dt").as[Double]
      // difficult ones
      val raw_I = (json \ "I_raw" \ "Y").as[Vector[Double]]
      val raw_V = (json \ "U_raw" \ "Y").as[Vector[Double]]
      val dtSmall = (json \ "U_raw" \ "dt").as[Double]
      // prepare results
      sequenceChecker(n)
      RawDataContainer(t, dt, ac_I, ac_V, dc_I, dc_V, phase, raw_I, raw_V , n)
    } catch {
      case e: Exception =>
        log.error(e, "TCP IV received information could not be praised using JSON");
        return null
    }
  }


  /** checks incoming data order numbers to make sure all elements are present */
  def sequenceChecker(addN: Int): Unit = {
    if ( lastN == -1 ) {} //init
    else if (lastN + 1 == addN) {} // normal
    else if (lastN == addN){
      log.warning("Received same IV info twice! Unhandled")
    } else if (lastN > addN){
      log.warning("The IV packets coming in wrong order! Unhandled ")
    } else if (lastN +1 < addN){
      log.warning("The IV packets were missing")
    }
    lastN = addN
  }


}

object IVDataActor {
  def props(): Props = Props(new IVDataActor() )

  case class RawDataContainer( val t: DateTime,
                               val dt: Double, // time interval between messages
                               val ac_I: Double,
                               val ac_V: Double,
                               val dc_I: Double,
                               val dc_V: Double,
                               val phase: Double, // phase difference between I anv V in ac
                               val raw_I: Vector[Double],
                               val raw_V: Vector[Double],
                               val cycle: Int
                               )


}