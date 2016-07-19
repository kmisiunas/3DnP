package com.misiunas.np.hardware.approach

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.approach.ApproachStage.ApproachStageStatus
import com.misiunas.np.hardware.communication.Communication.{SerialAsk, SerialReply, SerialTell}
import com.misiunas.np.hardware.communication.CommunicationTCP
import com.misiunas.np.hardware.logging.MotionLogger
import com.misiunas.np.hardware.stage.PiezoStage.{Move, MoveBy, PiezoStatus}
import com.misiunas.np.hardware.stage.{MoveWorker, StatusWorker}
import com.misiunas.np.tools.{Talkative, Wait}
import com.typesafe.config.ConfigFactory
import org.joda.time.DateTime

import scala.annotation.tailrec
import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask



/**
  * Mostly a coppy of PiezoStage
  *
  * Created by kmisiunas on 2016-06-30.
  */
class ApproachStage extends Actor with ActorLogging {

  // ## knowledge about piezo

  /** intended position for the stage */
  var r: Vec = Vec(0, 0, 0)

  /** last know parameters of a piezo stage */
  var status: ApproachStageStatus = ApproachStageStatus(DateTime.now, Vec(0,0,0), false)

  /** iz piezo stage online */
  var online: Boolean = false


  // ## Internal Cogs

  /** connection with the device */
  protected val tcp = context.actorOf(CommunicationTCP.propsForPiezoStage(), "tcp")

  /** actor for moving stage */
  lazy protected val mover = context.actorOf(
    MoveWorker.props(
      tcp,
      Vec(ConfigFactory.load.getDouble("approachStage.minPosition"), 0.0, 0.0),
      Vec(ConfigFactory.load.getDouble("approachStage.maxPosition"), 0.0, 0.0)
    )
    , "approachMover")

  /** actor for moving stage */
  lazy protected val updater = context.actorOf(StatusWorker.props(tcp), "approachStatus")

  import com.misiunas.np.hardware.stage.PiezoStage._

  // akka actors
  override def receive: Receive = {
    // special commands first
    case _ if !online =>
      log.warning("Approach Stage still offline at {}", DateTime.now.toString("HH:mm"))
    case "Reset Position" => r = status.pos // just propagating position
    case s: PiezoStatus if sender == updater =>
      status = ApproachStageStatus(s.lastUpdate, Vec(0,0,s.pos.x), s.moving) //remap
      log.debug("New approach stage status registered: {}", status)
    // control commands
    case Move(v) =>
      mover forward move(v)  //todo not sure we want this as forward, could be just tell !
      registerMotion()
    case MoveBy(dr) =>
      mover forward move(r + dr)
      registerMotion()
    case Stop =>
      tcp ! SerialTell("STP")
      mover ! Restart // todo: if it has a job in process - what happens?
      self ! "Reset Position"  // where did we stop
    case PositionQ => sender ! r
    case StatusQ => sender ! status
  }

  /** moves the stage */
  protected def move(newPos: Vec): Move = {
    if(newPos.x != 0.0 || newPos.y != 0.0)
      log.error("Approach stage can only move in Z axis. Vec invalid: " + newPos)
    r = Vec(0.0,0.0,newPos.z)
    Move( Vec( r.z, 0 , 0 ) ) // map to device axes
  }

  override def preStart() = init()

  /** initialise the stage */
  def init(): Unit = {
    val config = ConfigFactory.load
    // check if it is online
    @tailrec
    def checkForResponse(): Unit = {
      implicit val timeout = Timeout(1 seconds)
      val query = tcp ? SerialAsk("*IDN?")
      val reply = Await.result(query, timeout.duration).asInstanceOf[SerialReply].reply
      if (reply.isEmpty){
        Wait.stupid(1000)
        checkForResponse()
      } else if (reply.head.startsWith("Physik Instrumente")){
        online = true;
        log.info("Piezo Stage online with response: {}", reply.head)
        // report time it took to go online
        val t0 = System.currentTimeMillis()
        Talkative.getResponse(tcp, SerialAsk("*IDN?")) // don't care about result
        log.info("Piezo Stage ping time: "+ (System.currentTimeMillis()-t0) +" ms")
      } else {
        throw new Exception("Unknown response from Piezo Stage via TCP")
      }
    }
    checkForResponse()
    // Set closed-loop operation for all axes
    tcp ! SerialTell("SVO 1 1")

    // make sure everything was received
    Wait.stupid(100)
    updater ! "wakeup"
    Wait.stupid(500)
    mover ! "wakeup"
    import collection.JavaConversions._
    //val initPos = Vec( ConfigFactory.load.getDoubleList("piezo.initialPosition").toList.map(d => d.toDouble) )
    //mover ! Move(initPos)
  }

  // # Logger

  /** attempt to log motion */
  def registerMotion() = {
    context.actorSelection("/user/logger.motion") ! MotionLogger.Log
  }

}


object ApproachStage {

  // AKKA actor framework
  def props(): Props = Props(new ApproachStage() )

  // Methods for communicating with actor

  case class ApproachStageStatus( lastUpdate: DateTime,
                                  pos: Vec,
                                  moving: Boolean )

}