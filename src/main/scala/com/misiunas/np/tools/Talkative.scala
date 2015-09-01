package com.misiunas.np.tools

import akka.util.Timeout

import scala.concurrent.{Await, Future}

/**
 * # Helper methods for dealing with Akka actors
 *
 * Created by kmisiunas on 15-08-31.
 */
object Talkative {

  import scala.concurrent.duration._
  implicit val timeout = Timeout(2 seconds)

  /** waits until the response is generated (locks the thread) */
  def getResponse[T](f: Future[T]): T = Await.result(f, timeout.duration).asInstanceOf[T]


}
