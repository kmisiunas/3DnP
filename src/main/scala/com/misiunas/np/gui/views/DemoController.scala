package com.misiunas.np.gui.views

import javafx.fxml.FXML
import javafx.scene.control.TextArea

import com.misiunas.np.gui.DemoJavaFXApp
;

/**
 * Created by kmisiunas on 15-08-15.
 */
class DemoController {

  @FXML
  var textOut: TextArea = null

  var mainApp: DemoJavaFXApp = null

  /**
   * Initializes the controller class. This method is automatically called
   * after the fxml file has been loaded.
   */
  @FXML
  private def initialize() = {
    // Initialize the person table with the two columns.
    // textOut.setTex
  }

  def setMainApp(mainApp: DemoJavaFXApp) {
    this.mainApp = mainApp;

    // Add observable list data to the table
    //personTable.setItems(mainApp.getPersonData());
  }

}
