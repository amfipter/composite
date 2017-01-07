package amfipter.plugin

import org.eclipse.ui.plugin.AbstractUIPlugin
import org.osgi.framework.BundleContext


class Activator extends AbstractUIPlugin {
  val PLUGIN_ID = "CompositeConfigurationUI"
  
  
  override def start(context :BundleContext) :Unit = {
    super.start(context)
    Activator.plugin = this
  }
  
  override def stop(context :BundleContext) :Unit = {
    Activator.plugin = null 
    super.stop(context)
  }
  
}

object Activator {
  var plugin :Activator = null 
  def getDefault() :Activator = {
    return plugin
  }
}