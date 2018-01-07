package com.misiunas.np.essential

import akka.actor.ActorRef
import com.misiunas.geoscala.vectors.Vec
import akka.pattern.ask
import akka.util.Timeout
import com.misiunas.np.hardware.stage.PiezoStage.{Move, MoveBy, PositionQ}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import concurrent.duration._

/** # Linear position executor
  *
  * All lengths in um
  *
  * ## Current implementation: minimal function to see what we need
  *
  * Created by kmisiunas on 2016-07-24.
  */
class ProbePosition (val xyz: ActorRef, val approach: ActorRef){

  import collection.JavaConversions._
  private val max: Vec = Vec( ConfigFactory.load.getDoubleList("piezo.maxPosition").toList.map(d => d.toDouble) )


  def posGlobal: Vec = tryThis({
    implicit val timeout = Timeout(200 millisecond)
    val posPiezo = xyz ? PositionQ
    val posApproach = approach ? PositionQ
    Await.result(posPiezo, timeout.duration).asInstanceOf[Vec] +
      Await.result(posApproach, timeout.duration).asInstanceOf[Vec]
  })

  def pos: Vec = tryThis({
    implicit val timeout = Timeout(200 millisecond)
    Await.result(xyz ? PositionQ, timeout.duration).asInstanceOf[Vec]
  })

  def move(r: Vec): Vec = tryThis({
    implicit val timeout = Timeout(2 second)
    Await.result(xyz ? Move(r), timeout.duration)
    pos
  })

  def moveBy(dr: Vec): Vec = tryThis({
    implicit val timeout = Timeout(2 second)
    Await.result(xyz ? MoveBy(dr), timeout.duration)
    pos
  })

  def canMoveBy(dr: Vec): Boolean = tryThis({
    implicit val timeout = Timeout(200 millisecond)
    val posPiezo = Await.result(xyz ? PositionQ, timeout.duration).asInstanceOf[Vec]
    val r = posPiezo + dr
    r.x >= 0 && r.y>=0 && r.z>=0 && r.x<=max.x && r.y<=max.y && r.z<=max.z
  })

  def moveApproachStageBy(dr: Vec): Vec = tryThis[Vec]({
    implicit val timeout = Timeout(10 second)
    //throw new Exception("should not move!")
    Await.result(approach ? MoveBy(dr), timeout.duration)
    posGlobal
  })

  def moveByGuranteed(dr: Vec): Unit = tryThis(
    if (canMoveBy(dr)) moveBy(dr)
    else moveApproachStageBy(dr)
  )

  private def tryThis[A](fn: =>A): A = try{
    fn
  } catch {
    case e: RuntimeException => throw new PositionTimeoutException(e)
    case e => throw e
  }

}

object ProbePosition{
  def apply(xyz: ActorRef, approach: ActorRef): ProbePosition = new ProbePosition(xyz, approach)
}

class PositionTimeoutException(e: RuntimeException) extends RuntimeException(e.getMessage, e.getCause)
