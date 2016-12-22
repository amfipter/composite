package amfipter.plugin

import amfipter.plugin.CompositePluginException

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.core.DebugPlugin

import java.util.Vector
import java.util.ArrayList

import scala.util.Random
import scala.collection.mutable.ArrayBuffer


/** Launch configuration processing utils
 * 
 */
object ConfigurationHelper {
    
  /** Associate launch configuration with uniq identifier
   *  
   * @param launchConfiguration some launch configuration
   */
  def initId(launchConfiguration : ILaunchConfiguration, configurations :Array[Object]) :Unit = {
    val id = launchConfiguration.getAttribute(PluginConstants.storeIdPrefix, "")
    if( id.equals("")) {
      val wc = launchConfiguration.getWorkingCopy
      wc.setAttribute(PluginConstants.storeIdPrefix, getNewId(configurations))
      wc.doSave
    }
  }
  
  /** Association of the inner launch configuration representation and LaunchConfiguration
   *  
   * Using uniq configuration id instead of name
   */
  def findConfigurations(configurations :Vector[LaunchConfigurationElement]) :Unit = {
    val launchConfugurations = DebugPlugin.getDefault.getLaunchManager.getLaunchConfigurations
    for( launchConfuguration <- launchConfugurations) {
      val id = launchConfuguration.getAttribute(PluginConstants.storeIdPrefix, "")
      for(configurationIndex <- 0 until configurations.size) {
        if( configurations.get(configurationIndex).id.equals(id)) {
          configurations.get(configurationIndex).launchConfiguration = launchConfuguration
          configurations.get(configurationIndex).name = launchConfuguration.getName
//            break
        }
      }  
    }
  }
  
  /** Generate uniq string identifier 
   *  
   * @return random uniq alphanumeric string
   */
  def getNewId(configurations :Array[Object]) :String = {
    val usedId = new ArrayBuffer[String]
    configurations.map(x => usedId += x.asInstanceOf[LaunchConfigurationElement].id)
    val random = new Random
    var newId = random.alphanumeric.take(PluginConstants.configurationIdStringSize).mkString
    while(usedId.contains(newId)) {
      newId = random.alphanumeric.take(PluginConstants.configurationIdStringSize).mkString
    }
    newId
  }
  
  /** Provides cycle search in configuration dependencies
   * 
   * @return a tuple with answer and array of configuration names in cycle if possible
   */
  def findCycle(configurationCurrent :ILaunchConfiguration, compositeConfigurationType :ILaunchConfigurationType, configurations :Array[Object]) :(Boolean, Array[String]) = {
    val configurationStack = new ArrayBuffer[String]
    configurationStack += configurationCurrent.getName
    var cyclePath :Array[String] = null
    var cycle = false
    
    /** Depth-first search in composite configuration dependency graph 
     * 
     * @param configs array of composite's enclosed configurations
     */
    def DFS(configs :Array[ILaunchConfiguration]) :Unit = {
//      log(configurationStack)
      for( config <- configs) {
        if( configurationStack.contains(config.getName)) {
          cycle = true
          cyclePath = configurationStack.toArray
          return
        }
        if( config.getType.equals(compositeConfigurationType)) {
//          log(config.getName)
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
   * @param compositeConfig configuration with composite type
   * @return array of referring configurations 
   */
  private def getInnerConfigs(compositeConfig :ILaunchConfiguration, compositeConfigurationType :ILaunchConfigurationType) :Array[ILaunchConfiguration] = {
    if(!compositeConfig.getType.equals(compositeConfigurationType)) {
      throw new CompositePluginException("Wrong composite type")
    }
    val launchConfugurations = DebugPlugin.getDefault.getLaunchManager.getLaunchConfigurations.toArray
    val configs = new ArrayBuffer[ILaunchConfiguration]
    val storedData = compositeConfig.getAttribute(PluginConstants.storeAttributeName, null.asInstanceOf[ArrayList[String]])
    for( serializedLaunchElement <- storedData.toArray) {
      val lElement = new LaunchConfigurationElement(serializedLaunchElement.asInstanceOf[String])
      try {
        configs += launchConfugurations.filter(x => x.getAttribute(PluginConstants.storeIdPrefix, null.asInstanceOf[String]).equals(lElement.id))(0)
      } catch {
        case e :Throwable => throw new CompositePluginException("Configuration mismatch id")
      }
    }
    configs.toArray
  }
}