package amfipter.plugin.ui

import org.eclipse.osgi.util.NLS

class GuiConstants extends NLS {
  
  
  
}

object GuiConstants {
  private val BUNDLE_NAME = "amfipter.plugin.ui.guiconstants"
  NLS.initializeMessages(BUNDLE_NAME, GuiConstants.getClass())
  var buttonAdd = "ADD"
  var buttonRemove = "REMOVE"
  var buttonCopy = "COPY"
  var buttonUp = "UP"
  var buttonDown = "DOWN"
  val tableCol1Width = 100
  val tableCol2Width = 100
  val tableCol3Width = 100
  val tableCol4Width = 150
  val tableCol5Width = 100
  var tableCol1Name = "NAME"
  var tableCol2Name = "MODE"
  var tableCol3Name = "DELAY"
  var tableCol4Name = "WAIT TERMINATION"
  var tableCol5Name = "EXECUTION COUNT"
}