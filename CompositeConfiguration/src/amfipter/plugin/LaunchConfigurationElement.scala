package amfipter.plugin

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchManager

/** Execution mode (Run / Debug / Profile)
 * 
 */
object ExecutionMode extends Enumeration {
  val Run, Debug, Profile = Value
}

/** Configuration representation
 * 
 */
class LaunchConfigurationElement {
  var name = ""
  var mode = ExecutionMode.Run
  var execCount = 1
  var waitTermination = false
  var parallel = false
  var delay = 0 
  var launchConfiguration :ILaunchConfiguration = null
  var id = ""
  
  /** Restore configuration from serialized string
   * 
   * @param serialized Special-format string (serialized fields)
   */
  def this(serialized :String)  {
    this()
    val values = serialized.split(", ").toBuffer
    parallel = values.last.toBoolean
    values.trimEnd(1)
    id = values.last 
    values.trimEnd(1)
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
    name                = another.name
    mode                = another.mode
    execCount           = another.execCount
    waitTermination     = another.waitTermination
    delay               = another.delay
    id                  = another.id
    launchConfiguration = another.launchConfiguration
    parallel            = another.parallel
    
  }
  
  /** Serialize object fields
   * 
   * @return Special-format string (serialized fields)
   */
  def serialize() :String = {
    s"$name, $mode, $execCount, $waitTermination, $delay, $id, $parallel"
  }
  
  /** Get launch mode (canonical view)
   * 
   * @return Launch mode
   */
  def getMode() :String = {
    mode match {
      case ExecutionMode.Run => return ILaunchManager.RUN_MODE
      case ExecutionMode.Debug => return ILaunchManager.DEBUG_MODE
      case ExecutionMode.Profile => return ILaunchManager.PROFILE_MODE
    }
  }
  
  override def toString() :String = {
    s"LaunchConfigurationElement($name, $mode, $execCount, $waitTermination, $delay, $id)"
  }
}