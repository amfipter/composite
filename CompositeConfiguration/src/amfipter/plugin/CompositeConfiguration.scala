package test1

import org.eclipse.debug.core.model.ILaunchConfigurationDelegate
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunch
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.debug.core.model.IDebugTarget
import org.eclipse.debug.core.model.IProcess
import org.eclipse.debug.core.ILaunchManager //String mode comparison 

class CompositeConfiguration extends ILaunchConfigurationDelegate {
  
  override def launch(configuration :ILaunchConfiguration, mode: String, launch :ILaunch, monitor :IProgressMonitor) :Unit = {
    Unit
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
  
} 