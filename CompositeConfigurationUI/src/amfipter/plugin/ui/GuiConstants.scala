package amfipter.plugin.ui

import org.eclipse.osgi.util.NLS

class GuiConstants extends NLS {}

object GuiConstants {
  private val BUNDLE_NAME = "amfipter.plugin.ui.guiconstants"
  var buttonAdd = "ADD"
  var buttonRemove = "REMOVE"
  var buttonCopy = "COPY"
  var buttonUp = "UP"
  var buttonDown = "DOWN"
  val tableCol1Width = 200
  val tableCol2Width = 100
  val tableCol3Width = 100
  val tableCol4Width = 150
  val tableCol5Width = 150
  val tableCol6Width = 100
  var tableCol1Name = "NAME"
  var tableCol2Name = "MODE"
  var tableCol3Name = "DELAY"
  var tableCol4Name = "WAIT TERMINATION"
  var tableCol5Name = "EXECUTION COUNT"
  var tableCol6Name = "PARALLEL"
  val dialogWigth = 500
  val dialogHeight = 600
  val dialogOK = 0
  var dialodAdd = "Add new configurations"
  var cycleError = "Cycle error!"
  var cycleErrorDescription = "Next launch configurations create cycle."
  NLS.initializeMessages(BUNDLE_NAME, GuiConstants.getClass())
}