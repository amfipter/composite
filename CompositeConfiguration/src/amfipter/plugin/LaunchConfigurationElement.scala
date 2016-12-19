package amfipter.plugin

import org.eclipse.debug.core.ILaunchConfiguration

object ExecutionMode extends Enumeration {
  val Run, Debug, Profile = Value
}

class LaunchConfigurationElement {
  
  
  var name = ""
  var mode = ExecutionMode.Run
  var execCount = 1
  var waitTermination = false
  var delay = 0
  var launchConfiguration :ILaunchConfiguration = null
  
  def this(serialised :String)  {
    this()
    val values = serialised.split(", ").toBuffer   
    delay = values.last.toInt
    values -= values.last
    waitTermination = values.last.toBoolean
    values -= values.last
    execCount = values.last.toInt
    values -= values.last
    values.last match {
      case "Run" => mode = ExecutionMode.Run
      case "Debug" => mode = ExecutionMode.Debug
      case "Profile" => mode = ExecutionMode.Profile
    }
    values -= values.last
    name = values.mkString
  }
  
  def this(another :LaunchConfigurationElement) {
    this()
    name = another.name
    mode = another.mode
    execCount = another.execCount
    waitTermination = another.waitTermination
    delay = another.delay
    
  }
  def serialize() :String = {
    s"$name, $mode, $execCount, $waitTermination, $delay"
  }
  override def toString() :String = {
    s"LaunchConfigurationElement($name, $mode, $execCount, $waitTermination, $delay)"
  }
}