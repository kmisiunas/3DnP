package com.misiunas.np.essential

import akka.actor.Actor.Receive
import akka.actor.{Props, ActorRef, ActorLogging, Actor}

/**
 * ## Creates queue for items to execute
 *
 * Created by kmisiunas on 15-09-09.
 */
class Processor ( val xyz: ActorRef,
                  val iv: ActorRef,
                  val dac: ActorRef  ) extends Actor with ActorLogging {

  val amplifier: Amplifier = Amplifier.voltageMode(dac, iv)


  override def receive: Receive = {
    case p: Process[Any] =>
      //todo: check if everything ready
      //do the task
      log.info("starting Process: "+p)
      val res = p.start(xyz, amplifier)
      res match {
        case Some(out) => sender ! out
        case None => ()
      }
    case _ => log.warning("Message not understood.")
  }


}


object Processor {

  def props(xyz: ActorRef, iv: ActorRef, dac: ActorRef ): Props = Props( new Processor(xyz, iv, dac) )

}