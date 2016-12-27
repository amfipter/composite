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
//    monitor.beginTask(configuration.getName, 1)
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
        log(51)
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
          } else { log(currentLaunch.getLaunchConfiguration.getName);
            //processes += currentLaunch; log(currentLaunch.getLaunchConfiguration.getName)
            waitTermination(currentLaunch);  log(60)
          }
        }
        log(63)
        if( launchConfiguration.asInstanceOf[LaunchConfigurationElement].waitTermination) { processes.map(x => log(x.getLaunchConfiguration.getName))
          waitTermination(processes.toArray); 
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