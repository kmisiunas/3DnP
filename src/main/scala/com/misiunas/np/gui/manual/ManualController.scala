package com.misiunas.np.gui.manual

import java.net.Socket
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.{FXML, Initializable}
import javafx.scene.control._

import akka.actor.ActorRef
import com.misiunas.geoscala.vectors.Vec
import com.misiunas.np._
import com.misiunas.np.essential.Processor
import com.misiunas.np.essential.Processor.ProcessorDoNow
import com.misiunas.np.essential.processes._
import com.misiunas.np.essential.processes.minor.{ImagingDACSettings, StepAndPulse}
import com.misiunas.np.gui.tools.Fields
import com.misiunas.np.hardware.adc.control.DAC.{DepositionElectrode, ImagingElectrode}
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._

/**
 * ToDo:
 *  - actively synchronise the values
 *
 * Created by kmisiunas on 15-09-08.
 */
class ManualController extends Initializable {

  // # Views

  @FXML private var buttonStart: Button = null
  @FXML private var buttonPause: Button = null
  @FXML private var buttonStop: Button = null

  @FXML private var progressIndicator: ProgressIndicator = null

  @FXML private var approachSurfaceButton: Button = null
  @FXML private var findAndFollowSurfaceButton: Button = null

  //@FXML private var textCurrentProcess: Label = null
  @FXML private var listProcessQueue: ListView[String] = null

  // # Left Column - properties

  @FXML private var approachDistanceField: TextField = null
  @FXML private var approachSpeedField: TextField = null
  @FXML private var conductivityCheckIntervalField: TextField = null
  @FXML private var checkModeAC: CheckBox = null // not sure why this does not work!
  @FXML private var fieldRetreatDistance: TextField = null
  @FXML private var fieldScanSpacing: TextField = null
  @FXML private var depositionModeToggle: CheckBox = null
  @FXML private var fielddZ: TextField = null
  @FXML private var fieldPulseMagnitude: TextField = null

  // # Actions

  @FXML protected def actionStart(event: ActionEvent) = sendToActor( Processor.Start )
  @FXML protected def actionPause(event: ActionEvent) = sendToActor( Processor.Pause )
  @FXML protected def actionStop(event: ActionEvent) = sendToActor( Processor.Stop )

  @FXML protected def approachSurfaceAction(event: ActionEvent) =
    sendToActor( Approach.manual(
      target = approachDistanceField.getText.toDouble,
      stepsToConfirm = 5,
      speed = approachSpeedField.getText.toDouble
    ) )


  @FXML protected def findAndFollowSurfaceAction(event: ActionEvent) =
    sendToActor( KeepDistance(
      distanceFraction = approachDistanceField.getText.toDouble,
      approachSpeed = approachSpeedField.getText.toDouble,
      baselineCheckInterval = conductivityCheckIntervalField.getText.toDouble
    ))

  @FXML protected def actionSampleDeposition(event: ActionEvent) =
    sendToActor( SampleDeposition( dZ = approachDistanceField.getText.toDouble ) )

  @FXML protected def actionGridScan(event: ActionEvent) =
    sendToActor( GridScan( target = approachDistanceField.getText.toDouble ) )

  @FXML protected def setStandardDACSettingsAction(event: ActionEvent) =
    sendToActor( ImagingDACSettings() )

  @FXML protected def actionSafetyRetreat(event: ActionEvent) =
    sendToActor(SimpleProcess.probe(probe => probe.moveByGuranteed( Vec(0,0,-10) ) ))

  @FXML protected def zeroCurrentAction(event: ActionEvent) =
    sendToActor(SimpleProcess.amplifier( amp => amp.zeroCurrent() ))

  @FXML protected def depositPulseAction(event: ActionEvent) = {
    val dZ = fielddZ.getText.toDouble
    val pulseSize = fieldPulseMagnitude.getText.toDouble
    sendToActor(SimpleProcess.probe(probe => probe.moveBy( Vec(0,0,dZ) ) ))
    sendToActor(SimpleProcess.amplifier(amp => {amp.pulse( pulseSize )} ))
    sendToActor(SimpleProcess.probe(probe => probe.moveBy( Vec(0,0,-dZ) ) ))
  }

  @FXML protected def growPillarAction(event: ActionEvent) = {
    val dZ = fielddZ.getText.toDouble
    sendToActor( GrowPillar(dZ) )
  }

  @FXML protected def toggleModeAC(event: ActionEvent) ={
    val selected = checkModeAC.isSelected
    sendToActor( ProcessorDoNow( SimpleProcess.amplifier(amp => amp.trackModeAC(selected) ) ) )
  }

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
    Fields.onlyNumbers(fielddZ)
    Fields.onlyNumbers(fieldPulseMagnitude)


    val conf = ConfigFactory.load

    approachDistanceField.setText(""+ 0.85  )
    approachSpeedField.setText(""+ conf.getDouble("experiment.tipRadius")/4  )
    conductivityCheckIntervalField.setText(""+ 60  )

    fieldRetreatDistance.setText("4.0")
    fieldScanSpacing.setText("0.200")

//    checkboxModeAC.setSelected(false)
    depositionModeToggle.setSelected(true)
    listProcessQueue.setEditable(true)
    listProcessQueue.setItems( FXCollections.observableArrayList("No connection with processor") )
    progressIndicator.setOpacity(0.0)
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

  def setProcessorStatus(status: Processor.Status): Unit = {
    if (status.running) progressIndicator.setOpacity(1.0) //  progress indicator
    else progressIndicator.setOpacity(0.0) // no progress indicator
    status.queue match {
      case Nil =>
        listProcessQueue.setItems(FXCollections.observableArrayList("Empty"))
      case list: Seq[String] =>
        listProcessQueue.setItems(FXCollections.observableArrayList(list.asJava))
    }
  }




}
