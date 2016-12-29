package amfipter.plugin

import java.io.PrintWriter
import java.util.ArrayList
import java.util.Vector

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import org.eclipse.debug.core.DebugPlugin
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.ui.ILaunchGroup


/** Launch configuration processing utils
 * 
 */
object ConfigurationHelper {
    
  /** Associate launch configuration with uniq identifier
   *  
   * @param launchConfiguration Some launch configuration
   * @param configuration Array of LaunchConfigurationElement
   */
  def initId(launchConfiguration : ILaunchConfiguration, configurations :Array[Object]) :Unit = {
    val id = launchConfiguration.getAttribute(PluginConstants.STORE_ID_PREFIX, "")
    if( id.equals("")) {
      val wc = launchConfiguration.getWorkingCopy
      wc.setAttribute(PluginConstants.STORE_ID_PREFIX, getNewId(configurations))
      wc.doSave
    }
  }
  
  /** Association of the inner launch configuration representation and LaunchConfiguration
   *  
   * Using uniq configuration id instead of name
   * 
   * @param configuration Array of LaunchConfigurationElement
   */
  def findConfigurations(configurations :Vector[LaunchConfigurationElement]) :Unit = {
    val launchConfugurations = DebugPlugin.getDefault.getLaunchManager.getLaunchConfigurations    
    for( launchConfuguration <- launchConfugurations) {
      val id = launchConfuguration.getAttribute(PluginConstants.STORE_ID_PREFIX, "")
      for(configurationIndex <- 0 until configurations.size) {
        if( configurations.get(configurationIndex).id.equals(id)) {
          configurations.get(configurationIndex).launchConfiguration = launchConfuguration
          configurations.get(configurationIndex).name = launchConfuguration.getName
          
        }
      }  
    }
    val emptyConfigurations = configurations.toArray.filter(x => x.asInstanceOf[LaunchConfigurationElement].launchConfiguration.eq(null))
    
    for( deleted <- emptyConfigurations) {
      configurations.remove(deleted)
    }
  }
  
  /** Generate uniq string identifier 
   *  
   * @param configuration Array of LaunchConfigurationElement
   * @return Random uniq alphanumeric string
   */
  def getNewId(configurations :Array[Object]) :String = {
    val usedId = new ArrayBuffer[String]
    configurations.map(x => usedId += x.asInstanceOf[LaunchConfigurationElement].id)
    val random = new Random
    var newId = random.alphanumeric.take(PluginConstants.CONFIGURATION_ID_STRING_SIZE).mkString
    while(usedId.contains(newId)) {
      newId = random.alphanumeric.take(PluginConstants.CONFIGURATION_ID_STRING_SIZE).mkString
    }
    newId
  }
  
  /** Provides cycle search in configuration dependencies
   * 
   * @param configurationCurrent Reference to the current composite configuration 
   * @param compositeConfigurationType Composite configuration type
   * @param configurations Array of LaunchConfigurationElement
   * @return Tuple with answer and array of configuration names in cycle if possible
   */
  def findCycle(configurationCurrent :ILaunchConfiguration, compositeConfigurationType :ILaunchConfigurationType, configurations :Array[Object]) :(Boolean, Array[String]) = {
    val configurationStack = new ArrayBuffer[String]
    configurationStack += configurationCurrent.getName
    var cyclePath :Array[String] = null
    var cycle = false
    /** Depth-first search in composite configuration dependency graph 
     * 
     * @param configs Array of composite's enclosed configurations
     */
    def DFS(configs :Array[ILaunchConfiguration]) :Unit = {
      for( config <- configs) {
        if( configurationStack.contains(config.getName)) {
          cycle = true
          cyclePath = configurationStack.toArray
          return
        }

        if( config.getType.equals(compositeConfigurationType)) {
          val newConfigs = getInnerConfigs(config, compositeConfigurationType)
          
          if( newConfigs.size > 0 && !cycle) {
            configurationStack += config.getName
            DFS(newConfigs)
            configurationStack.trimEnd(1)
          }
        }
      }
    }
    DFS(for( element <- configurations.toArray) yield element.asInstanceOf[LaunchConfigurationElement].launchConfiguration)
    (cycle, cyclePath)
  }
  
  /** Get list of inner configurations of composite element
   *  
   * @param compositeConfig Configuration with composite type
   * @param compositeConfigurationType Composite configuration type
   * @return Array of referring configurations 
   */
  private def getInnerConfigs(compositeConfig :ILaunchConfiguration, compositeConfigurationType :ILaunchConfigurationType) :Array[ILaunchConfiguration] = {
    if(!compositeConfig.getType.equals(compositeConfigurationType)) {
      throw new CompositePluginException("Wrong composite type")
    }
    val launchConfugurations = DebugPlugin.getDefault.getLaunchManager.getLaunchConfigurations.toArray
    val configs = new ArrayBuffer[ILaunchConfiguration]
    val storedData = compositeConfig.getAttribute(PluginConstants.STORE_ATTRIBUTE_NAME, null.asInstanceOf[ArrayList[String]])
    
    for( serializedLaunchElement <- storedData.toArray) {
      val lElement = new LaunchConfigurationElement(serializedLaunchElement.asInstanceOf[String])
      
      try {
        configs += launchConfugurations.filter(x => x.getAttribute(PluginConstants.STORE_ID_PREFIX, "").equals(lElement.id))(0)
      } catch {
        case e :Throwable => return new Array[ILaunchConfiguration](0) //throw new CompositePluginException(e.getMessage + " Configuration mismatch id")
      }
    }
    configs.toArray
  }
  
  /** All configurations calculation (include repeating)
   * 
   * @param configurations Array of LaunchConfigurationElement
   * @return Configurations count
   */
  def configurationsCount(configurations :Vector[LaunchConfigurationElement]) :Int = {
    var count = 0
    for( element <- configurations.toArray) {
      count += element.asInstanceOf[LaunchConfigurationElement].execCount
    }
    count
  }
  
  /** Get specified launch group
   *  
   * @param launchMode Configuration launch mode
   * @return Launch group
   */
  def getLaunchGroup(launchMode :String) :ILaunchGroup = {
    val lGroups = DebugUITools.getLaunchGroups().toArray[ILaunchGroup]
    lGroups.filter(_.getMode().equals(launchMode))(0)
  }
}