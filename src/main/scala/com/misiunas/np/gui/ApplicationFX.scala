package com.misiunas.np.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage

import akka.actor.{ActorSystem, ActorLogging}
import akka.event.Logging
import com.misiunas.np.gui.xyz.XYZController
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * Main Window for controlling the 3D printer
 *
 * Created by kmisiunas on 15-08-15.
 */
class ApplicationFX extends Application {

  val system: ActorSystem = ActorSystem("3DnP")


  override def start(primaryStage: Stage) {
    primaryStage.setTitle("3DnP by Karolis Misiunas")
    primaryStage.getIcons().add( new Image(getClass.getResourceAsStream("/icon.png")  ));

    // Load root layout from fxml file.
    var loader = new FXMLLoader();
    loader.setLocation(getClass.getResource("/com/misiunas/np/gui/views/xyz/xyz_layout.fxml"));
    var rootLayout = loader.load.asInstanceOf[Pane]

    val xyz = loader.getController[XYZController]


    val piezo = system.actorOf(PiezoStage.props(), "piezo")
    val guiUpdater = system.actorOf(GuiActor.props(piezo, xyz), "gui")

    // Show the scene containing the root layout.
    val scene: Scene = new Scene(rootLayout)
    primaryStage.setScene(scene)
    primaryStage.show
  }


  override def stop(){
    super.stop()
    system.shutdown // quit safely
  }

}