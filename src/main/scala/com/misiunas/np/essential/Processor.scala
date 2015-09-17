package com.misiunas.np.essential

import akka.actor.Actor.Receive
import akka.actor._
import com.misiunas.np.essential.DeviceProcess.{Finished, Continue}

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

  val amplifier: Amplifier = Amplifier.voltageMode(dac, iv)

  case class Job(val process: DeviceProcess, sender: ActorRef)

  var jobQueue: List[Job] = Nil
  var killFlag: Boolean = false

  // # Internal Controls

  case class StartJob(job: Job)
  case class DoStep(job: Job)
  case class FinishJob(job: Job)



  override def receive: Receive = {
    case newJob: DeviceProcess =>
      if(jobQueue.isEmpty) self ! StartJob( Job(newJob, sender) )
      else  jobQueue  = jobQueue :+ Job(newJob, sender)

    case StartJob(job) =>
      job.sender ! Processor.Status(true)
      log.debug("Started new Process: "+job.process+", from: "+job.sender)
      job.process.start(xyz, amplifier)
      self ! DoStep(job)

    case DoStep(job) =>
      if(killFlag) {
        self ! FinishJob(job)
        killFlag = false
      }
      else job.process.step() match {
        case Continue => self ! DoStep(job)
        case Finished => self ! FinishJob(job)
      }

    case FinishJob(job) =>
      job.process.preStop()
      log.debug("Process sopped: "+job.process)
      job.sender ! Processor.Status(false)
      if(jobQueue.nonEmpty) {
        self ! StartJob( jobQueue.head )
        jobQueue = jobQueue.tail
      }

    case Processor.Kill =>
      log.info("Process kill order issued by: "+sender())
      killFlag = true

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