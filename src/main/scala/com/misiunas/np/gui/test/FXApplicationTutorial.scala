package com.misiunas.np.gui.test

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.Pane
import javafx.stage.Stage

/**
 * Created by kmisiunas on 15-08-31.
 */
class FXApplicationTutorial extends Application {

  @Override
  def start(primaryStage: Stage): Unit  = {
    var root: Pane = FXMLLoader.load(getClass().getResource("views/fx_application_tutorial.fxml"));


    var scene: Scene = new Scene(root);

    primaryStage.setTitle("FXML Welcome");
    primaryStage.setScene(scene);
    primaryStage.show();
  }

}
