package com.misiunas.np.gui.xyz

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.{TextField, Label}

import akka.actor.ActorRef
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * Created by kmisiunas on 15-09-01.
 */
class XYZController {

  // general methods

  private var inform: Option[ActorRef] = None
  def setActor(a: ActorRef): Unit = {inform = Some(a)}

  def formatPos(x: Double): String = "%1.3f".format(x)

  /** command to check the validity of the data */
  def areNumberFieldsValid(): Boolean = {
    // check and color
    def checkField(field: TextField): Boolean = {
      try {
        val number = field.getText.toDouble
        field.setStyle("-fx-text-inner-color: black;");
        true
      } catch {
        case e: Exception =>
          // highlight the field
          field.setStyle("-fx-text-inner-color: red;");
          false
      }
    }
    checkField(xPosField) && checkField(yPosField) && checkField(zPosField)
  }


  // # views

  // ## Labels

  @FXML private var xPos: Label = null
  def setXPos(x: Double) = xPos.setText( formatPos( x ) )

  @FXML private var yPos: Label = null
  def setYPos(x: Double) = yPos.setText( formatPos( x ) )

  @FXML private var zPos: Label = null
  def setZPos(x: Double) = zPos.setText( formatPos( x ) )

  // ## TextFields


  @FXML private var xPosField: TextField = null
  @FXML private var yPosField: TextField = null
  @FXML private var zPosField: TextField = null

  def setFieldPos(vector: Vec): Unit = {
    if( !xPosField.isFocused && !yPosField.isFocused && !zPosField.isFocused ) {
      xPosField.setText("" + vector.x)
      yPosField.setText("" + vector.y)
      zPosField.setText("" + vector.z)
      // hope it does not tiger event listener!
    }
  }

  @FXML protected def xPosAction(event: ActionEvent) = {
    // should I test event type?
    // Check data validity
    if( areNumberFieldsValid() ){ // If data is valid - issue a motion order
      val pos = Vec(xPosField.getText.toDouble, yPosField.getText.toDouble, zPosField.getText.toDouble)
      inform match {
        case Some(actor) => actor ! PiezoStage.Move(pos)
        case None => println("WARNING: no Actor was attached to XYZController ")
      }
    }
  }
  @FXML protected def yPosAction(event: ActionEvent) = xPosAction(event)
  @FXML protected def zPosAction(event: ActionEvent) = xPosAction(event)
  @FXML protected def validateAllFields(event: ActionEvent): Unit = { areNumberFieldsValid() }
}
