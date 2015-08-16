package com.misiunas.np

import javafx.application.Application

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.gui.DemoJavaFXApp
import com.misiunas.np.hardware.stage.{PiezoStatus, PiezoStage}
import com.misiunas.np.hardware.stage.TCPSimple.{TCPReply, TCPAsk}
import com.misiunas.np.tools.Wait
import com.typesafe.config.ConfigFactory
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by kmisiunas on 15-07-09.
 */
object Main extends App {


  def mainTest(args: Array[String]) {
    Application.launch(classOf[DemoJavaFXApp], args: _*)
  }
  //mainTest(args)

  println("testing config reads: piezo.tcp.readWait = "+ConfigFactory.load.getInt("piezo.tcp.readWait"))

  val system = ActorSystem("3DnP")

  println("Initiate Actor for connecting to Piezo Stage")
  val myActor = system.actorOf(PiezoStage.props(), "piezo")

  println("Actor alive")

  println("=> ask for status")
  implicit val timeout = Timeout(50 seconds)
  var reply = myActor ? PiezoStage.StatusQ
  var res = Await.result(reply, timeout.duration).asInstanceOf[PiezoStatus]
  println("<= got results: \n"+ res.pos)

  println("=> move to new loaction: Vec(10,10,10)")
  myActor ! PiezoStage.Move(Vec(10,10,10))

  Wait.stupid(1000)

  println("=> ask for Position")
  reply = myActor ? PiezoStage.PositionQ
  var pos = Await.result(reply, timeout.duration).asInstanceOf[Vec]
  println("<= got results: \n"+ pos)


  println("=> ask for status")
  reply = myActor ? PiezoStage.StatusQ
  res = Await.result(reply, timeout.duration).asInstanceOf[PiezoStatus]
  println("<= got results: \n"+ res.pos)

  println("shutting down")
  system.shutdown()


}