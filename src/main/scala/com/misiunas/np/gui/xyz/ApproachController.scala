package com.misiunas.np.gui.xyz

import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control.{CheckBox, Label, TextField}

import akka.actor.ActorRef
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np.gui.tools.Fields
import com.misiunas.np.hardware.approach.ApproachStage
import com.misiunas.np.hardware.stage.PiezoStage

/**
  * Created by kmisiunas on 2016-07-21.
  */
class ApproachController extends Initializable {

  def initialize(location: java.net.URL, resources: java.util.ResourceBundle) {
    Fields.onlyNumbers(stepSizeField)
  }


  // # Auto Injection Elements

  // ## TextFields


  @FXML private var zPosField: TextField = null
  @FXML private var stepSizeField: TextField = null

  // ## Labels

  @FXML private var zPos: Label = null

  // ## Checkboxes

  @FXML private var feedbackLoop: CheckBox = null

  @FXML private var online: CheckBox = null

  // # Actions

  // ## Step manipulators

  @FXML protected def actionStepUpZ(event: ActionEvent) =
    inform.get ! PiezoStage.MoveBy( Vec(0, 0, stepSize()) )
  @FXML protected def actionStepDownZ(event: ActionEvent) =
    inform.get ! PiezoStage.MoveBy( Vec(0, 0, -stepSize()) )

  // ## position inspection and setting

  @FXML protected def zPosAction(event: ActionEvent) = {
    // should I test event type?
    // Check data validity
    if( areNumberFieldsValid() ){ // If data is valid - issue a motion order
    val pos = Vec(0, 0, zPosField.getText.toDouble)
      inform match {
        case Some(actor) => actor ! PiezoStage.Move(pos)
        case None => println("WARNING: no Actor was attached to ApproachController ")
      }
    }
  }
  @FXML protected def validateAllFields(event: ActionEvent): Unit = { areNumberFieldsValid() }


  @FXML protected def feedbackLoopAction(event: ActionEvent): Unit = {
    inform match {
      case Some(actor) => actor ! ApproachStage.FeedbackLoop(feedbackLoop.isSelected)
      case None => println("WARNING: no Actor was attached to ApproachController ")
    }
  }



  // # general methods

  private var inform: Option[ActorRef] = None
  def setActor(a: ActorRef): Unit = {inform = Some(a)}

  def formatPos(x: Double): String = "%1.3f".format(x)

  def stepSize(): Double = stepSizeField.getText().toDouble

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
    checkField(zPosField)
  }


  // ## Labels setters

  def setZPos(z: Double) = zPos.setText( formatPos( z ) )


  def setFieldPos(vector: Vec): Unit = {
    if( !zPosField.isFocused ) {
      zPosField.setText(formatPos(vector.z))
      // hope it does not tiger event listener!
    }
  }

}
