package amfipter.plugin

import org.eclipse.debug.core.ILaunchConfiguration
import java.io.PrintWriter

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
  
  def this(serialized :String)  {
    this()
    val values = serialized.split(", ").toBuffer
    delay = values.last.toInt
    values.trimEnd(1)
    waitTermination = values.last.toBoolean
    values.trimEnd(1)
    execCount = values.last.toInt
    values.trimEnd(1)
    values.last match {
      case "Run" => mode = ExecutionMode.Run
      case "Debug" => mode = ExecutionMode.Debug
      case "Profile" => mode = ExecutionMode.Profile
    }
    values.trimEnd(1)
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