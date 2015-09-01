package com.misiunas.np.gui.test

import javafx.collections.ObservableList
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.{TableView, TextField}

/**
 * Created by kmisiunas on 15-08-31.
 */
class FXApplicationController_scala {
  @FXML private var tableView: TableView[Person] = null
  @FXML private var firstNameField: TextField = null
  @FXML private var lastNameField: TextField = null
  @FXML private var emailField: TextField = null

  @FXML protected def addPerson(event: ActionEvent) {
    val data: ObservableList[Person] = tableView.getItems
    data.add(new Person(firstNameField.getText, lastNameField.getText, emailField.getText))
    firstNameField.setText("")
    lastNameField.setText("")
    emailField.setText("")
  }
}