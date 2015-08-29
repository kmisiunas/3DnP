package com.misiunas.np

import javafx.application.Application

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.gui.DemoJavaFXApp
import com.misiunas.np.hardware.adc.input.TCPStreamReader
import com.misiunas.np.hardware.adc.input.test.TestTCP
import com.misiunas.np.hardware.stage.{TCPSimple, PiezoStatus, PiezoStage}
import com.misiunas.np.hardware.stage.TCPSimple.{TCPReply, TCPAsk}
import com.misiunas.np.tools.Wait
import com.typesafe.config.ConfigFactory
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by kmisiunas on 15-07-09.
 */
object Main extends App {

  implicit val timeout = Timeout(2 seconds)
  def getResponse[T](f: Future[T]): T = Await.result(f, timeout.duration).asInstanceOf[T]


  def mainTest(args: Array[String]) {
    Application.launch(classOf[DemoJavaFXApp], args: _*)
  }
  //mainTest(args)

  println("testing config reads: piezo.tcp.readWait = "+ConfigFactory.load.getInt("piezo.tcp.readWait"))

  val system = ActorSystem("3DnP")

  println("Initiate Actor for connecting to Piezo Stage")
  val piezo = system.actorOf(PiezoStage.props(), "piezo")

  println("Actor alive")

  println("=> ask for status")
  var reply: Future[Any] = piezo ? PiezoStage.StatusQ
  var res = Await.result(reply, timeout.duration).asInstanceOf[PiezoStatus]
  println("<= got results: \n"+ res.pos)

  Wait.stupid(1000)

  println("=> move to new loaction: Vec(10,10,10)")
  piezo ! PiezoStage.Move(Vec(10,10,10))

  Wait.stupid(4000)

  println("=> ask for Position")
  reply = piezo ? PiezoStage.PositionQ
  var pos = Await.result(reply, timeout.duration).asInstanceOf[Vec]
  println("<= got results: \n"+ pos)


  println("=> ask for status")
  reply = piezo ? PiezoStage.StatusQ
  res = Await.result(reply, timeout.duration).asInstanceOf[PiezoStatus]
  println("<= got results: \n"+ res.pos)

  println("shutting down")
  system.shutdown()

  val tcpReader = system.actorOf(TestTCP.props(), "tcpReader")
}