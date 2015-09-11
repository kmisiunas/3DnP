package com.misiunas.np.gui.test

import javafx.application.Application
import javafx.beans.property.{SimpleStringProperty, StringProperty}
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.image.Image
import javafx.scene.layout.Pane
import javafx.stage.Stage

import com.misiunas.np.gui.test.DemoController

/**
 * Created by kmisiunas on 15-08-15.
 */
class DemoJavaFXApp extends Application {

  private var textOut: TextArea = null

  private var outMsg: StringProperty = new SimpleStringProperty("before init");

  def setText(st: String): Unit = {
    outMsg.setValue(st)
  }

  override def start(primaryStage: Stage) {
    DemoJavaFXApp.gui = this // todo: Ugly testing implementation!!!!
    primaryStage.setTitle("Testing JavaFX!")
    primaryStage.getIcons().add( new Image(getClass.getResourceAsStream("/icon.png") ));

    // Load root layout from fxml file.
    var loader = new FXMLLoader();
    loader.setLocation(getClass.getResource("/com/misiunas/np/gui/test/Demo.fxml"));
    var rootLayout = loader.load.asInstanceOf[Pane]
    textOut = rootLayout.lookup("#textOut").asInstanceOf[TextArea]
    textOut.setText("early")

    // Give the controller access to the main app.
    val controller: DemoController = loader.getController()
    controller.setMainApp(this)

    // Show the scene containing the root layout.
    val scene: Scene = new Scene(rootLayout)
    primaryStage.setScene(scene)
    primaryStage.show
  }


}

object DemoJavaFXApp {

  protected var gui: DemoJavaFXApp = null

  def setText(st: String) = gui.setText(st)


}