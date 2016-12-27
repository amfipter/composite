package amfipter.plugin.ui

import org.eclipse.osgi.util.NLS

//class GuiConstants extends NLS {}

object GuiConstants extends NLS {
  private val BUNDLE_NAME = "amfipter.plugin.ui.guiconstants" 
  var buttonAdd = "Add"
  var buttonRemove = "Remove"
  var buttonCopy = "Copy"
  var buttonUp = "Up"
  var buttonDown = "Down"
  val TABLE_COL1_WIDTH = 200
  val TABLE_COL2_WIDTH = 100
  val TABLE_COL3_WIDTH = 100
  val TABLE_COL4_WIDTH = 150
  val TABLE_COL5_WIDTH = 150
  val TABLE_COL6_WIDTH = 100
  var tableCol1Name = "Name"
  var tableCol2Name = "Mode"
  var tableCol3Name = "Delay"
  var tableCol4Name = "Wait Termination"
  var tableCol5Name = "Execution Count"
  var tableCol6Name = "Parallel"
  val DIALOG_WIDTH = 500
  val DIALOG_HEIGHT = 600
  val DIALOG_OK = 0
  var dialodAdd = "Add new configurations"
  var cycleError = "Cycle error!"
  var cycleErrorDescription = "Next launch configurations create cycle."
  var loadError = "Launch configuration error"
  var loadErrorDescription = "Can't load configuration"
  def init() :Unit = NLS.initializeMessages(BUNDLE_NAME, GuiConstants.getClass)
}