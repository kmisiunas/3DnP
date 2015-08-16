package com.misiunas.np.gui

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage

/**
 * Main Window for controlling the 3D printer
 *
 * Created by kmisiunas on 15-08-15.
 */
class ControlFXApp extends Application {

  override def start(primaryStage: Stage) {
    primaryStage.setTitle("3DnP by Karolis Misiunas")
    primaryStage.getIcons().add( new Image(getClass.getResourceAsStream("/icon.png")  ));

    // Load root layout from fxml file.
    var loader = new FXMLLoader();
    loader.setLocation(getClass.getResource("/com/misiunas/np/gui/views/Demo.fxml"));
    var rootLayout = loader.load.asInstanceOf[Pane]

    // Show the scene containing the root layout.
    val scene: Scene = new Scene(rootLayout)
    primaryStage.setScene(scene)
    primaryStage.show
  }

}