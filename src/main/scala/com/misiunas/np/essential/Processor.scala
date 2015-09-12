package com.misiunas.np.essential

import akka.actor.Actor.Receive
import akka.actor._
import com.misiunas.np.essential.implementations.ProcessorWorker

/**
 * # Creates queue for items to execute
 *
 * About killing other actors
 * http://stackoverflow.com/questions/13847963/akka-kill-vs-stop-vs-poison-pill
 *
 * Created by kmisiunas on 15-09-09.
 */
class Processor ( val xyz: ActorRef,
                  val iv: ActorRef,
                  val dac: ActorRef  ) extends Actor with ActorLogging {

  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ActorKilledException     => Restart
      case _: Exception                => Escalate
    }


  val amplifier: Amplifier = Amplifier.voltageMode(dac, iv)

  val worker: ActorRef = context.actorOf(ProcessorWorker.props(xyz, amplifier), "worker")

  override def receive: Receive = {
    case Processor.Kill =>
      worker ! Kill
      sender ! Processor.Status(false)
      ???
    case p: DeviceProcess[Any] => worker forward p
    case _ => log.warning("Message not understood.")
  }


}


object Processor {

  def props(xyz: ActorRef, iv: ActorRef, dac: ActorRef ): Props = Props( new Processor(xyz, iv, dac) )

  /** Method for suddenly stopping the DeviceProcess that was running */
  case object Kill

  /** status of the processor */
  case class Status(working: Boolean)

}