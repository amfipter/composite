package amfipter.plugin.ui

import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup
import org.eclipse.debug.ui.CommonTab
import org.eclipse.debug.ui.ILaunchConfigurationDialog
import org.eclipse.debug.ui.ILaunchConfigurationTab
import scala.collection.mutable.ArrayBuffer

class CompositeTabGroup extends AbstractLaunchConfigurationTabGroup {
  
  override def createTabs(dialog :ILaunchConfigurationDialog, mode :String): Unit = {
    val tabs = ArrayBuffer[ILaunchConfigurationTab]()
    
    tabs += new CompositeTab(mode)
    tabs += new CommonTab
    
    setTabs(tabs.toArray)
 
  }
  
}