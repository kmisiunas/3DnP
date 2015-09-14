package com.misiunas.np.hardware.stage

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{ActorRef, Actor, ActorLogging, Props}
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.TCPSimple
import TCPSimple.{TCPReply, TCPAsk, TCPTell}
import com.misiunas.np.hardware.logging.MotionLogger
import com.misiunas.np.hardware.stage.PiezoStage.PiezoStatus
import com.misiunas.np.tools.{Talkative, Wait}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime
import akka.pattern.ask
import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * # Access and Control to Piezo XYZ stage
 *
 * ## Design
 *
 * - Make sure this layer is responsive
 * - Lockable processes are sent to workers
 *
 * ## ToDo
 *
 * - graceful offline mode
 *
 * Created by kmisiunas on 15-08-02.
 */
class PiezoStage extends Actor with ActorLogging {

  // ## knowledge about piezo

  /** intended position for the stage */
  var r: Vec = Vec(0, 0, 0)

  /** last know parameters of a piezo stage */
  var status: PiezoStatus = PiezoStatus(DateTime.now, Vec(0,0,0), false)

  /** iz piezo stage online */
  var online: Boolean = false


  // ## Internal Cogs

  /** connection with the device */
  protected val tcp = context.actorOf(TCPSimple.propsForPiezoStage(), "tcp")

  /** actor for moving stage */
  lazy protected val mover = context.actorOf(MoverWorker.props(tcp), "piezoMover")

  /** actor for moving stage */
  lazy protected val updater = context.actorOf(StatusWorker.props(tcp), "piezoStatus")

  import PiezoStage._

  // akka actors
  override def receive: Receive = {
    // special commands first
    case _ if !online =>
      log.warning("Piezo Stage still offline at {}", DateTime.now.toString("HH:mm"))
    case "Reset Position" => r = status.pos // just propagating position
    case s: PiezoStatus if sender == updater =>
      status = s
      log.debug("New piezo status registered: {}", s)
    // control commands
    case Move(v) =>
      mover forward move(v)  //todo not sure we want this as forward, could be just tell !
      registerMotion()
    case MoveBy(dr) =>
      mover forward move(r + dr)
      registerMotion()
    case Stop =>
      tcp ! TCPTell("STP")
      mover ! Restart // todo: if it has a job in process - what happens?
      self ! "Reset Position"  // where did we stop
    case PositionQ => sender ! r
    case StatusQ => sender ! status
  }

  /** moves the stage */
  protected def move(newPos: Vec): Move = {
    r = newPos
    Move(r)
  }

  override def preStart() = init()

  /** initialise the stage */
  def init(): Unit = {
    val config = ConfigFactory.load
    // check if it is online
    @tailrec
    def checkForResponse(): Unit = {
      implicit val timeout = Timeout(1 seconds)
      val query = tcp ? TCPAsk("*IDN?")
      val reply = Await.result(query, timeout.duration).asInstanceOf[TCPReply].reply
      if (reply.isEmpty){
        Wait.stupid(1000)
        checkForResponse()
      } else if (reply.head.startsWith("Physik Instrumente")){
        online = true;
        log.info("Piezo Stage online with response: {}", reply.head)
        // report time it took to go online
        val t0 = System.currentTimeMillis()
        Talkative.getResponse(tcp, TCPAsk("*IDN?")) // don't care about result
        log.info("Piezo Stage ping time: "+ (System.currentTimeMillis()-t0) +" ms")
      } else {
        throw new Exception("Unknown response from Piezo Stage via TCP")
      }
    }
    checkForResponse()
    // Set closed-loop operation for all axes
    tcp ! TCPTell("SVO 1 1 2 1 3 1")
    // Set max motion velocity
    tcp ! TCPTell(
      "VEL 1 {xy} 2 {xy} 3 {z}"
        .replace("{xy}", config.getDouble("piezo.maxXYSpeed").toFloat.toString )
        .replace("{z}" , config.getDouble("piezo.maxZSpeed" ).toFloat.toString )
    )
    // make sure everything was received
    Wait.stupid(100)
    updater ! "wakeup"
    Wait.stupid(500)
    mover ! "wakeup"
    import collection.JavaConversions._
    val initPos = Vec( ConfigFactory.load.getDoubleList("piezo.initialPosition").toList.map(d => d.toDouble) )
    mover ! Move(initPos)
  }

  // # Logger

  /** attempt to log motion */
  def registerMotion() = {
    context.actorSelection("/user/logger.motion") ! MotionLogger.Log
  }

}


object PiezoStage {

  // AKKA actor framework
  def props(): Props = Props(new PiezoStage() )

  // Methods for communicating with actor

  case class Move(val r: Vec)

  case class MoveBy(val dr: Vec)

  case object Stop

  case object PositionQ
  case object StatusQ

  case class PiezoStatus ( lastUpdate: DateTime,
                           pos: Vec,
                           moving: Boolean )

}