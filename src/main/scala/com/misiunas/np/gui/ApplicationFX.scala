package com.misiunas.np.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.image.Image
import javafx.scene.layout.{Pane, VBox}
import javafx.stage.Stage

import akka.actor.{ActorLogging, ActorSystem}
import akka.event.Logging
import com.misiunas.np.essential.Processor
import com.misiunas.np.gui.manual.{ManualController, ManualView}
import com.misiunas.np.gui.xyz.{ApproachController, ApproachView, XYZController, XYZView}
import com.misiunas.np.hardware.adc.control.DAC
import com.misiunas.np.hardware.adc.input.IV
import com.misiunas.np.hardware.approach.ApproachStage
import com.misiunas.np.hardware.logging.MotionLogger
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * Main Window for controlling the 3D printer
 *
 * Created by kmisiunas on 15-08-15.
 */
class ApplicationFX extends Application {

  // Vital system components

  val system: ActorSystem = ActorSystem("3DnP")
  val xyz = system.actorOf(PiezoStage.props(), "piezo")
  val approach = system.actorOf(ApproachStage.props(), "approach")
  val iv = system.actorOf(IV.props(), "iv")
  val control = system.actorOf(DAC.props(), "dac")
  val motionLogger = system.actorOf(MotionLogger.props(iv, xyz), "logger.motion")
  val processor = system.actorOf(Processor.props(xyz,approach,iv,control), "processor")


  override def start(primaryStage: Stage) = {
    //todo: test if components started successfully
    primaryStage.setTitle("3DnP by Karolis Misiunas")
    primaryStage.getIcons().add( new Image(getClass.getResourceAsStream("/icon.png")  ))
    // prepare to load elements
    var loader = new FXMLLoader()
    // Scrollable contained for all interface parts
    val scrollPane: ScrollPane = new ScrollPane()
    val content: VBox = new VBox()
    scrollPane.setContent(content)
    scrollPane.setPrefSize(610, 800)
    // add approach stage
    loader.setLocation(getClass.getResource("/com/misiunas/np/gui/xyz/approach_layout.fxml"))
    val approachPane: Pane = loader.load.asInstanceOf[Pane]
    val approachController = loader.getController[ApproachController]
    content.getChildren().add(approachPane)
    val aproachView = system.actorOf(ApproachView.props( approach , approachController), "gui.approach")
    // add Piezo controls
    loader = new FXMLLoader()
    loader.setLocation(getClass.getResource("/com/misiunas/np/gui/xyz/xyz_layout.fxml"))
    val xyzPane: Pane = loader.load.asInstanceOf[Pane]
    val xyzController = loader.getController[XYZController]
    content.getChildren().add(xyzPane)
    val xyzView = system.actorOf(XYZView.props(xyz, xyzController), "gui.xyz")
    // add Manual controls
    loader = new FXMLLoader()
    loader.setLocation(getClass.getResource("/com/misiunas/np/gui/manual/manual_layout.fxml"))
    val manualPane = loader.load.asInstanceOf[Pane]
    val manualController = loader.getController[ManualController]
    content.getChildren().add(manualPane)
    val manualView = system.actorOf(ManualView.props(processor, manualController), "gui.manual")

    // Show the scene containing the root layout
    primaryStage.setScene(new Scene(scrollPane))
    primaryStage.show
  }


  override def stop(){
    super.stop()
    system.shutdown // quit safely
  }

}