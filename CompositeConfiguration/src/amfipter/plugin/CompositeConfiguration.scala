package amfipter.plugin

import amfipter.plugin.PluginConstants

import org.eclipse.debug.core.model.ILaunchConfigurationDelegate
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.core.ILaunch
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.debug.core.model.IDebugTarget
import org.eclipse.debug.core.model.IProcess
import org.eclipse.debug.core.ILaunchManager //String mode comparison 
import scala.collection.mutable.ArrayBuffer
import java.util.ArrayList

class CompositeConfiguration extends ILaunchConfigurationDelegate {
  private var configurations = new ArrayBuffer[ILaunchConfiguration]
  private var configurationName :String = null
  private var configurationType :ILaunchConfigurationType = null
  private var configurationCurrent :ILaunchConfiguration = null
  
  override def launch(configuration :ILaunchConfiguration, mode: String, launch :ILaunch, monitor :IProgressMonitor) :Unit = {
    configurationName = configuration.getName
    configurationType = configuration.getType
    configurationCurrent = configuration
  }
  
  def launchInnerConfiguration(configuration :ILaunchConfiguration, mode: String, launch :ILaunch, monitor :IProgressMonitor) :Unit = {
    val configurationLaunch :ILaunch = configuration.launch(mode, monitor)
    for(debugTarget :IDebugTarget <- configurationLaunch.getDebugTargets()) {
      launch.addDebugTarget(debugTarget)
    }
    for(process :IProcess <- configurationLaunch.getProcesses()) {
      launch.addProcess(process)
    }
  }
  
  private def initConfigurations() :Unit = {
    val storedData = configurationCurrent.getAttribute(PluginConstants.storeAttributeName, new ArrayList[String])
    
  }
  
} 