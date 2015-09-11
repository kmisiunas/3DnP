package com.misiunas.np.tools

import akka.actor.ActorRef
import akka.util.Timeout

import scala.concurrent.{Await, Future}
import akka.pattern.{ ask, pipe }

/**
 * # Helper methods for dealing with Akka actors
 *
 * Created by kmisiunas on 15-08-31.
 */
object Talkative {

  import scala.concurrent.duration._
  implicit val timeout = Timeout(5 seconds)

  /** waits until the response is generated (locks the thread) */
  def getResponse[T](actor: ActorRef, message: Any): T = Await.result(actor ? message, timeout.duration).asInstanceOf[T]


}