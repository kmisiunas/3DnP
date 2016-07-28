package com.misiunas.np.gui.manual

import java.net.Socket
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control._

import akka.actor.ActorRef
import com.misiunas.np._
import com.misiunas.np.essential.Processor
import com.misiunas.np.essential.processes._
import com.misiunas.np.essential.processes.minor.{ImagingDACSettings, StepAndPulse}
import com.misiunas.np.gui.tools.Fields
import com.misiunas.np.hardware.adc.control.DAC.{DepositionElectrode, ImagingElectrode}
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

  @FXML private var textCurrentProcess: Label = null

  // # Left Column - properties

  @FXML private var approachDistanceField: TextField = null
  @FXML private var approachSpeedField: TextField = null
  @FXML private var conductivityCheckIntervalField: TextField = null
  @FXML private var checkboxModeAC: CheckBox = null
  @FXML private var fieldRetreatDistance: TextField = null
  @FXML private var fieldScanSpacing: TextField = null
  @FXML private var depositionModeToggle: CheckBox = null


  // # Actions

  @FXML protected def killProcessorAction(event: ActionEvent) = sendToActor( Processor.Kill )

  @FXML protected def approachSurfaceAction(event: ActionEvent) =
    sendToActor( Approach( target = approachDistanceField.getText.toDouble ) )


  @FXML protected def findAndFollowSurfaceAction(event: ActionEvent) =
    sendToActor( KeepDistance(
      distanceFraction = approachDistanceField.getText.toDouble,
      approachSpeed = approachSpeedField.getText.toDouble,
      baselineCheckInterval = conductivityCheckIntervalField.getText.toDouble
    ))


  @FXML protected def actionGridScan(event: ActionEvent) =
    sendToActor( GridScan( target = approachDistanceField.getText.toDouble ) )

  @FXML protected def setStandardDACSettingsAction(event: ActionEvent) =
    sendToActor( ImagingDACSettings() )

  @FXML protected def zeroCurrentAction(event: ActionEvent) =
    sendToActor(SimpleProcess.amplifier( amp => amp.zeroCurrent() ))

  @FXML protected def depositPulseAction(event: ActionEvent) =
    sendToActor(SimpleProcess.amplifier(amp => amp.pulse( ??? ))) // todo set pulse size

  @FXML protected def toggleModeAC(event: ActionEvent) =
    sendToActor(SimpleProcess.amplifier(amp => amp.trackModeAC(checkboxModeAC.isSelected) ))

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
    Fields.onlyNumbers(fieldRetreatDistance)
    Fields.onlyNumbers(fieldScanSpacing)


    val conf = ConfigFactory.load

    approachDistanceField.setText(""+ 0.85  )
    approachSpeedField.setText(""+ conf.getDouble("experiment.tipRadius")/4  )
    conductivityCheckIntervalField.setText(""+ 60  )

    fieldRetreatDistance.setText("4.0")
    fieldScanSpacing.setText("0.200")

//    checkboxModeAC.setSelected(false)
    depositionModeToggle.setSelected(true)

    setProcessorStatus("") // no process initially
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

  def setProcessorStatus(process: String): Unit =
    process match {
      case "" =>
        progressIndicator.setOpacity(0.0)  // no progress indicator
        killProcessorButton.setDisable(true) // no process
        textCurrentProcess.setText("No process running")
      case _ =>
        progressIndicator.setOpacity(1.0)  //  progress indicator
        killProcessorButton.setDisable(false) //  process kill allowed
        textCurrentProcess.setText(process)
    }



}
