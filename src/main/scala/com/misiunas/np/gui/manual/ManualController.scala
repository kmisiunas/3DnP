package com.misiunas.np.gui.manual

import java.net.Socket
import javafx.event.ActionEvent
import javafx.fxml.{Initializable, FXML}
import javafx.scene.control._

import akka.actor.ActorRef
import com.misiunas.np._
import com.misiunas.np.essential.Processor
import com.misiunas.np.essential.processes.{KeepDistance, Approach}
import com.misiunas.np.essential.processes.minor.ImagingDACSettings
import com.misiunas.np.gui.tools.Fields
import com.typesafe.config.ConfigFactory

/**
 * Created by kmisiunas on 15-09-08.
 */
class ManualController extends Initializable {

  // # Views

  @FXML private var progressIndicator: ProgressIndicator = null
  @FXML private var killProcessorButton: Button = null

  @FXML private var approachSurfaceButton: Button = null
  @FXML private var findAndFollowSurfaceButton: Button = null

  @FXML private var approachDistanceField: TextField = null
  @FXML private var approachSpeedField: TextField = null
  @FXML private var conductivityCheckIntervalField: TextField = null
  @FXML private var logToFileToggle: CheckBox = null
  @FXML private var useACToggle: CheckBox = null
  @FXML private var imagingModeToggle: CheckBox = null

  // # Actions

  @FXML protected def killProcessorAction(event: ActionEvent) = sendToActor( Processor.Kill )

  @FXML protected def approachSurfaceAction(event: ActionEvent) =
    sendToActor( Approach(
      target = approachDistanceField.getText.toDouble,
      speed = approachSpeedField.getText.toDouble
    ) )


  @FXML protected def findAndFollowSurfaceAction(event: ActionEvent) =
    sendToActor( KeepDistance(
      distanceFraction = approachDistanceField.getText.toDouble,
      approachSpeed = approachSpeedField.getText.toDouble
    ))

  @FXML protected def setStandardDACSettingsAction(event: ActionEvent) =
    sendToActor( ImagingDACSettings() )


  // ## Other Actions

  @FXML protected def imagingModeAction(event: ActionEvent) = {}

  @FXML protected def logToFileAction(event: ActionEvent) = {}

  // # Init

  def initialize(location: java.net.URL, resources: java.util.ResourceBundle) {
    Fields.onlyNumbers(approachDistanceField)
    Fields.onlyNumbers(approachSpeedField)
    Fields.onlyNumbers(conductivityCheckIntervalField)

    val conf = ConfigFactory.load

    approachDistanceField.setText(""+ 0.85  )
    approachSpeedField.setText(""+ conf.getDouble("experiment.tipRadius")/4  )
    conductivityCheckIntervalField.setText(""+ 30  )

    logToFileToggle.setSelected(false)
    useACToggle.setSelected(false)
    imagingModeToggle.setSelected(true)

    setProcessorStatus(false) // no process initially
  }



  // # Other Methods

  // Actor ref
  private var inform: Option[ActorRef] = None
  def setActor(a: ActorRef): Unit = {inform = Some(a)}
  def sendToActor(msg: Any): Unit = {
    inform match {
      case Some(actor) => actor ! msg
      case None => println("ERROR: no Actor was attached to ManualController ")
    }
  }

  def setProcessorStatus(active: Boolean): Unit = {
    active match {
      case true =>
        progressIndicator.setOpacity(1.0)  // no progress indicator
        killProcessorButton.setDisable(false) // no process
      case false =>
        progressIndicator.setOpacity(0.0)  // no progress indicator
        killProcessorButton.setDisable(true) // no process
    }
  }

}
