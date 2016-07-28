package com.misiunas.np.essential

import akka.actor.Actor.Receive
import akka.actor._
import com.misiunas.np.essential.DeviceProcess._

/**
  * # Creates queue for items to execute
  *
  * ## Design:
  *  - Break down all tasks into steps
  *  - Allow to prepend any task with a sub-job (reuse code for Process'es)
  *
  * ## Consider:
  *  - adding a messaging option between jobs
  *  - adding finished job report message
  *
  * About killing other actors
  * http://stackoverflow.com/questions/13847963/akka-kill-vs-stop-vs-poison-pill
  *
  * Created by kmisiunas on 15-09-09.
  */
class Processor ( val xyz: ActorRef,
                  val approach: ActorRef,
                  val iv: ActorRef,
                  val dac: ActorRef  ) extends Actor with ActorLogging {

  val amplifier: Amplifier = Amplifier.voltageMode(dac, iv)
  val probe: ProbePosition = ProbePosition(xyz, approach)

  case class Job(val process: DeviceProcess, sender: ActorRef, started: Boolean)

  var jobQueue: List[Job] = Nil
  var killFlag: Boolean = false

  // # Internal Controls

  case class StartJob(job: Job)
  case class DoStep(job: Job)
  case class FinishJob(job: Job)



  override def receive: Receive = {

    case newJob: DeviceProcess =>
      if(jobQueue.isEmpty) self ! StartJob( Job(newJob, sender, false) )
      else  jobQueue  = jobQueue :+ Job(newJob, sender, false)

    case StartJob(job) =>
      job.sender ! Processor.Status(job.process.toString)
      log.debug("Started new Process: "+job.process+", from: "+job.sender)
      if (!job.started) job.process.start(probe, amplifier)
      self ! DoStep( Job(job.process, job.sender, started = true) )

    case DoStep(job) =>
      if(killFlag) {
        self ! FinishJob(job)
        killFlag = false
      }
      else job.process.step() match {
        case Continue =>
          self ! DoStep(job)
          job.sender ! Processor.Status(job.process.toString) // todo might be too many updates
        case Finished => self ! FinishJob(job)
        case InjectProcess(process) =>
          jobQueue = job +: jobQueue
          self ! StartJob( Job(process, job.sender, false) )
        case Panic(msg) =>
          self ! FinishJob(job)
          log.warning("Process "+job.process+"panicked: "+msg)
          jobQueue = Nil
          job.sender ! Processor.Status("")
      }

    case FinishJob(job) =>
      job.process.finalise() match {
        case DeviceProcess.Success =>
          log.debug("Process finished: "+job.process)
          job.sender ! Processor.Status("")
          if(jobQueue.nonEmpty) {
            self ! StartJob( jobQueue.head )
            jobQueue = jobQueue.tail
          }
      }

    case Processor.Kill =>
      log.info("Processes kill order issued by: "+sender())
      jobQueue = Nil
      killFlag = true

    case _ => log.warning("Message not understood.")
  }


}


object Processor {

  def props(xyz: ActorRef, approach: ActorRef, iv: ActorRef, dac: ActorRef ): Props =
    Props( new Processor(xyz, approach, iv, dac) )

  /** Method for suddenly stopping the DeviceProcess that was running */
  case object Kill

  /** status of the processor */
  case class Status(working: String)

}