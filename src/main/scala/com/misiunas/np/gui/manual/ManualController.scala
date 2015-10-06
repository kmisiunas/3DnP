package com.misiunas.np.gui.manual

import java.net.Socket
import javafx.event.ActionEvent
import javafx.fxml.{Initializable, FXML}
import javafx.scene.control._

import akka.actor.ActorRef
import com.misiunas.np._
import com.misiunas.np.essential.Processor
import com.misiunas.np.essential.processes.{SimpleProcess, KeepDistance, Approach}
import com.misiunas.np.essential.processes.minor.{StepAndPulse, ImagingDACSettings}
import com.misiunas.np.gui.tools.Fields
import com.misiunas.np.hardware.adc.control.DAC.{ImagingElectrode, DepositionElectrode}
import com.typesafe.config.ConfigFactory

/**
 * ToDo:
 *  - actively synchronise the values
 *
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
  @FXML private var pulseSizeField: TextField = null
  @FXML private var useACToggle: CheckBox = null
  @FXML private var depositionModeToggle: CheckBox = null
  @FXML private var hStepField: TextField = null

  @FXML private var genericField: TextField = null

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
      approachSpeed = approachSpeedField.getText.toDouble,
      baselineCheckInterval = conductivityCheckIntervalField.getText.toDouble
    ))

  @FXML protected def setStandardDACSettingsAction(event: ActionEvent) =
    sendToActor( ImagingDACSettings() )

  @FXML protected def depositPulseAction(event: ActionEvent) =
    sendToActor(SimpleProcess.amplifier(amp => amp.pulse(getPulseSize())))

  @FXML protected def stepAndPulseAction(event: ActionEvent) =
    sendToActor(StepAndPulse(hStepField.getText.toDouble, getPulseSize()))


  @FXML protected def zeroCurrentAction(event: ActionEvent) =
    sendToActor(SimpleProcess.amplifier( amp => amp.zeroCurrent() ))

  // ## Other Actions

  @FXML protected def depositionModeAction(event: ActionEvent) = {
    if(depositionModeToggle.isSelected) {
      sendToActor(SimpleProcess.amplifier(amp => amp.setElectrodeMode(DepositionElectrode)))
    } else {
      sendToActor(SimpleProcess.amplifier(amp => amp.setElectrodeMode(ImagingElectrode)))
    }
  }

  // # Init

  def initialize(location: java.net.URL, resources: java.util.ResourceBundle) {
    Fields.onlyNumbers(approachDistanceField)
    Fields.onlyNumbers(approachSpeedField)
    Fields.onlyNumbers(conductivityCheckIntervalField)
    Fields.onlyNumbers(pulseSizeField)
    Fields.onlyNumbers(hStepField)


    val conf = ConfigFactory.load

    approachDistanceField.setText(""+ 0.85  )
    approachSpeedField.setText(""+ conf.getDouble("experiment.tipRadius")/4  )
    conductivityCheckIntervalField.setText(""+ 60  )

    pulseSizeField.setText("100.0")
    hStepField.setText("20.0")

    useACToggle.setSelected(false)
    depositionModeToggle.setSelected(true)

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

  def getPulseSize(): Double =
    if( pulseSizeField == null ) 0.0
    else pulseSizeField.getText.toDouble

}
