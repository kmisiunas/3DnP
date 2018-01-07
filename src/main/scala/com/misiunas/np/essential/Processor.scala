package com.misiunas.np.essential

import akka.actor.Actor.Receive
import akka.actor._
import com.misiunas.np.essential.DeviceProcess._
import com.misiunas.np.essential.Processor.ProcessorDoNow

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
  var pause: Boolean = true
  var killFlag: Boolean = false

  // # Internal Controls

  case object StartNextJob
  case object DoStep
  case class FinishJob(job: Job)



  override def receive: Receive = {
    case StartNextJob =>
      val job = jobQueue.head
      log.debug("Started new Process: "+job.process+", from: "+job.sender)
      if (!job.started) {
        job.process.start(probe, amplifier)
        jobQueue = Job(job.process, job.sender, started = true) +: jobQueue.tail
      }
      job.process.onResume()
      self ! DoStep

    case DoStep =>
      val job = jobQueue.head
      if(killFlag) {
        self ! FinishJob(job)
      } else if (pause){
        job.process.onPause()
      }
      else {
        try {
          job.process.step() match {
            case Continue =>
              self ! DoStep
            case Finished => self ! FinishJob(job)
            case InjectProcess(process) =>
              jobQueue = Job(process, job.sender, false) +: jobQueue
              job.process.onPause()
              self ! StartNextJob
            case Panic(msg) =>
              self ! FinishJob(job)
              jobQueue = Nil
              log.warning("Process "+job.process+" panicked: "+msg)
          }
        } catch {
          case e: PositionTimeoutException => //non lethal exception - could not move
            log.warning("Process "+job.process+" threw non-crytical exception: "+e)
            job.process.onPause()
            self ! StartNextJob

          case e: Exception =>
            self ! FinishJob(job)
            jobQueue = Nil
            log.warning("Process "+job.process+" threw unknown exception: "+e)
        }
      }

    case FinishJob(job) =>
      job.process.onPause()
      job.process.onStop() match {
        case DeviceProcess.Success =>
          log.debug("Process finished: "+job.process)
          if (jobQueue.contains(job)) jobQueue = jobQueue.filterNot(_ == job) // remove
          if (jobQueue.nonEmpty) self ! StartNextJob // autostart next
          else pause = true // otherwise stop all
      }

    // External controls

    case ProcessorDoNow(process) =>
      // todo not great implementation
      jobQueue = List( Job(process, sender, false) )
      self ! Processor.Start

    case newJob: DeviceProcess =>
      jobQueue  = jobQueue :+ Job(newJob, sender, false)

    case Processor.Start =>
      killFlag = false; pause = false
      self ! StartNextJob

    case Processor.Pause =>
      pause = true

    case Processor.Stop =>
      killFlag = true; pause = true
      log.info("Processes kill order issued by: "+sender())
      jobQueue = Nil

    case Processor.StatusQ =>
      val list: List[String] =
        jobQueue.map(job => job.process.toString + (if(job.started) "  (started)" else "") )
      sender ! Processor.Status(list, !pause)

    case other => log.error("Message not understood: "+other)
  }


}


object Processor {

  def props(xyz: ActorRef, approach: ActorRef, iv: ActorRef, dac: ActorRef ): Props =
    Props( new Processor(xyz, approach, iv, dac) )

  trait Command

  /** Method for suddenly stopping the DeviceProcess that was running */
  case object Stop extends Command
  case object Pause extends Command
  case object Start extends Command
  case object Kill extends Command // todo

  case class ProcessorDoNow(pr: DeviceProcess)

  /** status of the processor */
  case class Status(queue: List[String], running: Boolean)

  case object StatusQ

}