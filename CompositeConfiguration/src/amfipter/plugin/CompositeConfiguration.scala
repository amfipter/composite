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
import java.util.Vector
import java.io.PrintWriter

class CompositeConfiguration extends ILaunchConfigurationDelegate {
  private var configurations = new Vector[LaunchConfigurationElement]
  private var configurationName :String = null
  private var configurationType :ILaunchConfigurationType = null
  private var configurationCurrent :ILaunchConfiguration = null
  
  class Logger(fileName :String) {
    val log = new PrintWriter(fileName)
    def println(x :Any) :Unit = {
      log.println(x.toString())
      log.flush()
    }
    
    def apply(x :Any) :Unit = {
      println(x)
    }
    
  }
  private val log = new Logger("launch")
  
  override def launch(configuration :ILaunchConfiguration, mode: String, launch :ILaunch, monitor :IProgressMonitor) :Unit = {
    configurationName = configuration.getName
    configurationType = configuration.getType
    configurationCurrent = configuration
    val processes = new ArrayBuffer[ILaunch]()
    initConfigurations
    log(configurations)
    for( launchConfiguration <- configurations.toArray) {
      if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].delay > 0) {
        Thread.sleep(launchConfiguration.asInstanceOf[LaunchConfigurationElement].delay)
      }
      
      for( i <- 0 until launchConfiguration.asInstanceOf[LaunchConfigurationElement].execCount) {
        val launch = launchConfiguration.asInstanceOf[LaunchConfigurationElement].launchConfiguration.launch(mode, null)
        if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].parallel) {
          processes += launch
        } else {
          processes += launch
          waitTermination(launch)
        }
      }
      if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].waitTermination) {
        waitTermination(processes.toArray)
      }
      processes.clear
    }
    
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
    val storedData = configurationCurrent.getAttribute(PluginConstants.storeAttributeName, new ArrayList[String])
    for( element <- storedData.toArray) {
      configurations.add(new LaunchConfigurationElement(element.asInstanceOf[String]))
    }
    ConfigurationHelper.findConfigurations(configurations)
  }
  
} 