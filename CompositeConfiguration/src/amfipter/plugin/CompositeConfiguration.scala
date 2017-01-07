package amfipter.plugin

import java.io.PrintWriter
import java.util.ArrayList
import java.util.Vector


import scala.collection.mutable.ArrayBuffer

import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.SubMonitor
import org.eclipse.debug.core.ILaunch
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate

class CompositeConfiguration extends ILaunchConfigurationDelegate {
  private var configurations                              = new Vector[LaunchConfigurationElement]
  private var configurationName :String                   = null
  private var configurationType :ILaunchConfigurationType = null
  private var configurationCurrent :ILaunchConfiguration  = null
  
  override def launch(configuration :ILaunchConfiguration, mode: String, launch :ILaunch, monitor :IProgressMonitor) :Unit = {
    configurationName    = configuration.getName
    configurationType    = configuration.getType
    configurationCurrent = configuration
    val processes = new ArrayBuffer[ILaunch]()
    initConfigurations
    try {
      val localMonitor = SubMonitor.convert(monitor,configuration.getName, ConfigurationHelper.configurationsCount(configurations))
      for( launchConfiguration <- configurations.toArray if !monitor.isCanceled) {
        if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].delay > 0) {
          Thread.sleep(launchConfiguration.asInstanceOf[LaunchConfigurationElement].delay)
        }
        for( i <- 0 until launchConfiguration.asInstanceOf[LaunchConfigurationElement].execCount) {    
          val currentLaunch = launchConfiguration.asInstanceOf[LaunchConfigurationElement].launchConfiguration.launch(
              launchConfiguration.asInstanceOf[LaunchConfigurationElement].getMode(), localMonitor.newChild(1))
          for( debugTarget <- currentLaunch.getDebugTargets) {
            launch.addDebugTarget(debugTarget)
          }
          for( process <- currentLaunch.getProcesses) {
            launch.addProcess(process)
          }
          
          if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].parallel || launchConfiguration.asInstanceOf[LaunchConfigurationElement].execCount == 1) {
            processes += currentLaunch
          } else { 
            waitTermination(currentLaunch)
          }
        }
        if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].waitTermination) { 
          waitTermination(processes.toArray)
        }
        processes.clear
      }
    } finally {
      monitor.done
    }    
  }
  
  private def waitTermination(process :ILaunch) :Unit = {
    while(!process.isTerminated) {
      Thread.sleep(1)
    }
  }
  
  private def waitTermination(arrayProcesses :Array[ILaunch]) :Unit = {
    for( process <- arrayProcesses) {
      waitTermination(process)
    }
  }
  
  private def initConfigurations() :Unit = {
    configurations = new Vector[LaunchConfigurationElement]
    var storedData :java.util.List[String] = null
    
    try {
      storedData = configurationCurrent.getAttribute(PluginConstants.STORE_ATTRIBUTE_NAME, new ArrayList[String])
    } catch {
      case e :Throwable => {
        storedData = new ArrayList[String]
        //can't read configuration
      }
    }
    for( element <- storedData.toArray) {
      configurations.add(new LaunchConfigurationElement(element.asInstanceOf[String]))
    }
    ConfigurationHelper.findConfigurations(configurations)
  }
  
} 