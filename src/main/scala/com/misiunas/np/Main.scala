package com.misiunas.np

import javafx.application.Application

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import breeze.macros.expand.args
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.gui.ApplicationFX
import com.misiunas.np.gui.test.FXApplicationTutorial
import com.misiunas.np.hardware.TCPSimple
import com.misiunas.np.hardware.adc.input.{IV, TCPStreamReader}
import com.misiunas.np.hardware.stage.{PiezoStatus, PiezoStage}
import TCPSimple.{TCPReply, TCPAsk}
import com.misiunas.np.tools.Wait
import com.typesafe.config.ConfigFactory
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

/**
 * Created by kmisiunas on 15-07-09.
 */
object Main extends App {

  // Application.launch(classOf[DemoJavaFXApp], args: _*)

  Application.launch(classOf[ApplicationFX], args: _* )


}