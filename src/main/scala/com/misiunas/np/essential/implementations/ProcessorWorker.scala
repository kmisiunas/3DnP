package com.misiunas.np.essential.implementations

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, ActorLogging, Actor}
import com.misiunas.np.essential.{Processor, DeviceProcess, Amplifier}

/**
 * Created by kmisiunas on 15-09-11.
 */
class ProcessorWorker (xyz: ActorRef, amplifier: Amplifier) extends Actor with ActorLogging {

  override def receive: Receive = {
    case process: DeviceProcess[Any] =>
      sender ! Processor.Status(true)
      log.debug("Starting Process: "+process)
      val res = process.start(xyz, amplifier)
//      res match {
//        case Some(out) => sender ! out
//        case None => ()
//      }
      sender ! Processor.Status(false)
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    // todo: clear inbox
    postStop()
  }

}



object ProcessorWorker {

  def props(xyz: ActorRef, amplifier: Amplifier): Props =
    Props( new ProcessorWorker(xyz, amplifier))

}