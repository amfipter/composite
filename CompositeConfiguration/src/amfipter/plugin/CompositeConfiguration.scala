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
import org.eclipse.core.runtime.SubMonitor

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
    try {
      val localMonitor = SubMonitor.convert(monitor,configuration.getName, ConfigurationHelper.configurationsCount(configurations))
      for( launchConfiguration <- configurations.toArray if !monitor.isCanceled) {
        if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].delay > 0) {
          Thread.sleep(launchConfiguration.asInstanceOf[LaunchConfigurationElement].delay)
        }
        
        for( i <- 0 until launchConfiguration.asInstanceOf[LaunchConfigurationElement].execCount) {    
          val currentLaunch = launchConfiguration.asInstanceOf[LaunchConfigurationElement].launchConfiguration.launch(mode, localMonitor.newChild(1))
          
          if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].parallel) {
            processes += currentLaunch
          } else {
            processes += currentLaunch
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
    val storedData = configurationCurrent.getAttribute(PluginConstants.storeAttributeName, new ArrayList[String])
    for( element <- storedData.toArray) {
      configurations.add(new LaunchConfigurationElement(element.asInstanceOf[String]))
    }
    ConfigurationHelper.findConfigurations(configurations)
  }
  
} 