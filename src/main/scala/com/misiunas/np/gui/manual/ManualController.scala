package com.misiunas.np.gui.manual

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.{Button, ProgressIndicator, Label}

import akka.actor.ActorRef
import com.misiunas.np.essential.Approach
import com.misiunas.np.hardware.stage.PiezoStage

/**
 * Created by kmisiunas on 15-09-08.
 */
class ManualController {

  // # Views

  @FXML private var progressIndicator: ProgressIndicator = null

  @FXML private var approachSurfaceButton: Button = null

  @FXML protected def approachSurfaceAction(event: ActionEvent) = {
    val process = Approach.auto()
    inform match {
      case Some(actor) => actor ! process
      case None => println("ERROR: no Actor was attached to ManualController ")
    }
  }

  // # Other Methods

  // Actor ref
  private var inform: Option[ActorRef] = None
  def setActor(a: ActorRef): Unit = {inform = Some(a)}

}
