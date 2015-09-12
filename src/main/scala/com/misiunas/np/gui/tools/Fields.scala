package com.misiunas.np.gui.tools

import javafx.beans.value.{ObservableValue, ChangeListener}
import javafx.scene.control.TextField

import spire.syntax.field

/**
 * # Tools for fileds
 *
 * Created by kmisiunas on 15-09-11.
 */
object Fields {

  /** only allow numbers as input */
  def onlyNumbers(field: TextField): Unit = {
    field.textProperty().addListener(new ChangeListener[String]() {
      @Override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String) {
        try{
          newValue.toDouble
        } catch {
          case e: Exception =>
            val pos = field.getCaretPosition()
            field.setText(oldValue)
            field.positionCaret(pos-1)
        }
      }
    })
  }

}
