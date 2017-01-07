package amfipter.plugin.ui


import java.io.PrintWriter
import java.util.ArrayList
import java.util.Vector

import scala.collection.mutable.ArrayBuffer

import org.eclipse.core.runtime.CoreException
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.Status
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.core.ILaunchManager
import org.eclipse.debug.internal.ui.DebugUIPlugin
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationFilteredTree
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupFilter
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab
import org.eclipse.jface.dialogs.Dialog
import org.eclipse.jface.dialogs.ErrorDialog
import org.eclipse.jface.viewers.ArrayContentProvider
import org.eclipse.jface.viewers.CellEditor
import org.eclipse.jface.viewers.CheckboxCellEditor
import org.eclipse.jface.viewers.ColumnLabelProvider
import org.eclipse.jface.viewers.ComboBoxCellEditor
import org.eclipse.jface.viewers.EditingSupport
import org.eclipse.jface.viewers.ISelectionChangedListener
import org.eclipse.jface.viewers.ITreeSelection
import org.eclipse.jface.viewers.SelectionChangedEvent
import org.eclipse.jface.viewers.TableLayout
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.jface.viewers.TableViewerColumn
import org.eclipse.jface.viewers.TextCellEditor
import org.eclipse.jface.viewers.Viewer
import org.eclipse.jface.viewers.ViewerFilter
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.activities.WorkbenchActivityHelper
import org.eclipse.ui.dialogs.PatternFilter

import amfipter.plugin.CompositePluginException
import amfipter.plugin.ConfigurationHelper
import amfipter.plugin.ExecutionMode
import amfipter.plugin.LaunchConfigurationElement
import amfipter.plugin.PluginConstants

/** Composite Tab GUI
 *
 */
class CompositeTab(lMode :String) extends AbstractLaunchConfigurationTab {
  private val launchMode                                  = lMode
  private var configurations                              = new Vector[LaunchConfigurationElement]
  private var configurationName                           = ""
  private var configurationType :ILaunchConfigurationType = null
  private var configurationCurrent :ILaunchConfiguration  = null 
  
  GuiConstants.init
 
  /** Support GUI object
   * 
   * Provides all GUI operations
   */
  private object CompositeTabGui {
    var tableViewer :TableViewer     = null
    var mainComposite :Composite     = null
    
    private var buttonAdd :Button    = null
    private var buttonRemove :Button = null
    private var buttonCopy :Button   = null
    private var buttonUp :Button     = null
    private var buttonDown :Button   = null 
    private var selectedConfigurations :ITreeSelection = null
    
    /** Dialog class that provides adding new launch configuration
     *  
     * @param parentShell Application shell
     * @param parentMode Launch mode (etc. run/debug..)
     */
    private class AddDialog(parentShell :Shell, parentMode :String) extends Dialog(parentShell) {
      val manager      = DebugUIPlugin.getDefault.getLaunchConfigurationManager
      val launchGroups = manager.getLaunchGroups
      val mode         = parentMode
      val launchGroup  = ConfigurationHelper.getLaunchGroup(mode)  
      val filter       = new ViewerFilter() {
        override def select(viewer :Viewer, parentElement :Object, element :Object) :Boolean = {
          if( element.isInstanceOf[ILaunchConfigurationType]) {
            return getLaunchManager.getLaunchConfigurations(element.asInstanceOf[ILaunchConfigurationType]).length > 0
          } else if( element.isInstanceOf[ILaunchConfiguration]) {
            return DebugUIPlugin.doLaunchConfigurationFiltering(element.asInstanceOf[ILaunchConfiguration]) && 
              !WorkbenchActivityHelper.filterItem(element.asInstanceOf[ILaunchConfiguration]) &&
              !configurationName.equals(element.asInstanceOf[ILaunchConfiguration].getName)
          } else 
            return false
        }
      }
      
      override protected def createDialogArea(parent :Composite) :Control = {
        val container = super.createDialogArea(parent).asInstanceOf[Composite]       
        val lTree     = new LaunchConfigurationFilteredTree(parent, 
            SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, 
            new PatternFilter(), launchGroup, null)
        lTree.createViewControl
        val filters   = lTree.getViewer.getFilters
        for( filter <- filters) {
          if( filter.isInstanceOf[LaunchGroupFilter]) {
            lTree.getViewer.removeFilter(filter)
          }
        }
        
        lTree.getViewer.setFilters(filter)
        lTree.getViewer.addSelectionChangedListener(new ISelectionChangedListener() {
          override def selectionChanged(event :SelectionChangedEvent) :Unit = {
            selectedConfigurations = lTree.getViewer.getStructuredSelection 
          }
        })
        
            
        container
      }
      override protected def configureShell(shell :Shell) :Unit = {
        super.configureShell(shell)
        shell.setText(GuiConstants.dialodAdd)
      }
      override protected def getInitialSize() :Point = {
        new Point(GuiConstants.DIALOG_HEIGHT, GuiConstants.DIALOG_WIDTH)
      }
    }
    

    /** Creates dialog to adding new launch configuration 
     * 
     * @param button Target button
     */
    def buttonAddAction(button :Button) :Unit = {
      buttonAdd = button
      button.addSelectionListener(new SelectionListener() {
        
        def widgetSelected(event :SelectionEvent) :Unit = {
          val dialog = new AddDialog(mainComposite.getShell, launchMode)
          dialog.create
          
          if( dialog.open == GuiConstants.DIALOG_OK) {                
            for( configuration <- selectedConfigurations.toArray if configuration.isInstanceOf[ILaunchConfiguration]) {
              val launchElement = new LaunchConfigurationElement()
              
              launchElement.name = configuration.asInstanceOf[ILaunchConfiguration].getName
              launchElement.launchConfiguration = configuration.asInstanceOf[ILaunchConfiguration]             
              ConfigurationHelper.initId(configuration.asInstanceOf[ILaunchConfiguration], configurations.toArray)
              launchElement.id = configuration.asInstanceOf[ILaunchConfiguration].getAttribute(PluginConstants.STORE_ID_PREFIX, "")
              var cycle :(Boolean, Array[String]) = null
              
              configurations.add(launchElement)
              try {
                cycle = ConfigurationHelper.findCycle(configurationCurrent, configurationType, configurations.toArray)
              } catch {
                case e :CompositePluginException => {
                  //plugin can't add new configuration
                  configurations.remove(launchElement)
                  return
                }
              }
              
              if(cycle._1.equals(true)) {
                val cycleElements = new StringBuilder
                
                for( element <- cycle._2) {
                  cycleElements ++= element
                  cycleElements ++= " -> \n"
                }
                cycleElements ++= configurationCurrent.getName
                val status = new Status(IStatus.ERROR, "amfipter.plugin.ui", cycleElements.mkString)
                
                ErrorDialog.openError(mainComposite.getShell, GuiConstants.cycleError, GuiConstants.cycleErrorDescription, status)
                configurations.remove(launchElement)
              }
            }
          }
          tableViewer.refresh()     //scala don't allow call refresh without brackets 
          updateButtons
          updateLaunchConfigurationDialog 
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit ={}
      })
    } 
    
    /** Remove selected table's elements
     * 
     * @param button Target button
     */
    def buttonRemoveAction(button :Button) :Unit = {
      buttonRemove = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection
          
          for(element <- selected.toArray) {
            configurations.remove(element.asInstanceOf[LaunchConfigurationElement])
          }
          tableViewer.refresh()   //scala don't allow call refresh without brackets 
          updateButtons
          updateLaunchConfigurationDialog
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit ={}
      })
    }
    
    /** Copy selected table's elements
     * 
     * @param button Target button
     */
    def buttonCopyAction(button :Button) :Unit = {
      buttonCopy = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection
          
          for(element <- selected.toArray) {
            val copy = new LaunchConfigurationElement(element.asInstanceOf[LaunchConfigurationElement])
            val position = configurations.indexOf(element.asInstanceOf[LaunchConfigurationElement])
            
            configurations.insertElementAt(copy, position)
          }
          tableViewer.refresh()    //scala don't allow call refresh without brackets 
          updateButtons
          updateLaunchConfigurationDialog
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
      })
      
    }
    
    /** Shift selected table's elements up
     * 
     * @param button Target button
     */
    def buttonUpAction(button :Button) :Unit = {
      buttonUp = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection
          
          for( configuration <- selected.toArray if configurations.indexOf(configuration) > 0) {
            val position = configurations.indexOf(configuration)
            val element = configurations.get(position)
            
            configurations.remove(position)
            configurations.insertElementAt(element, position - 1)
          }
          tableViewer.refresh()    //scala don't allow call refresh without brackets 
          updateButtons
          updateLaunchConfigurationDialog
          
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
      })
      
    }
    
    /** Shift selected table's elements down
     * 
     * @param button Target button
     */
    def buttonDownAction(button :Button) :Unit = {
      buttonDown = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection
          
          for( configuration <- selected.toArray.reverse if configurations.indexOf(configuration) < configurations.size - 1) {
            val position = configurations.indexOf(configuration)
            val element  = configurations.get(position)
            
            configurations.remove(position)
            configurations.insertElementAt(element, position + 1)
          }
          tableViewer.refresh()    //scala don't allow call refresh without brackets 
          updateButtons
          updateLaunchConfigurationDialog
        }
        
        def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
       })
    }
    
    /** Update buttons activity
     * 
     */
    def updateButtons() :Unit = {
      val selected = tableViewer.getStructuredSelection
      
      if (selected.size == 0) {
        buttonRemove.setEnabled(false)
        buttonCopy.setEnabled(false)
        buttonUp.setEnabled(false) 
        buttonDown.setEnabled(false)
        return
      }
      buttonRemove.setEnabled(true)
      buttonCopy.setEnabled(true)
      if( selected.size == 1 && configurations.indexOf(selected.getFirstElement) == 0) {
        buttonUp.setEnabled(false)
      } else if( selected.size > 1 && !selected.toArray.filter(x => configurations.indexOf(x) == 0).isEmpty) {
        buttonUp.setEnabled(false)
      } else {
        buttonUp.setEnabled(true)
      }
      if( selected.size == 1 && configurations.indexOf(selected.getFirstElement) == configurations.size - 1) {
        buttonDown.setEnabled(false)
      } else if( selected.size > 1 && !selected.toArray.filter(x => configurations.indexOf(x) == configurations.size - 1).isEmpty) {
        buttonDown.setEnabled(false)
      } else {
        buttonDown.setEnabled(true)
      }
    }
    
    /** Get selected elements in table and correct buttons activity
     * 
     */
    def tableSelectAction() :Unit = {
      tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        def selectionChanged(event :SelectionChangedEvent) :Unit = {
          updateButtons
        }
      })
    }
    
    /** Update main table data with new array
     * 
     */
    def updateTableData() :Unit = {
      tableViewer.setInput(configurations)
    }
    
    /** Generate error message
     * 
     */
    def configLoadError() :Unit = {
      ErrorDialog.openError(mainComposite.getShell, GuiConstants.loadError, GuiConstants.loadErrorDescription, null)
    }
    
    /** Editing support class for mode column
     *  
     * @param viewer Table viewer
     */
    class ModeEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
      val tableViewer = viewer
      
      override protected def getCellEditor(element :Object) :CellEditor = {
        val modes = new ArrayBuffer[String]
        val configuration = element.asInstanceOf[LaunchConfigurationElement].launchConfiguration
        
        if( configuration.supportsMode(ILaunchManager.RUN_MODE)) {
          modes += GuiConstants.modeRun
        }
        if( configuration.supportsMode(ILaunchManager.DEBUG_MODE)) {
          modes += GuiConstants.modeDebug
        }
        if( configuration.supportsMode(ILaunchManager.PROFILE_MODE)) {
          modes += GuiConstants.modeProfile
        }
        new ComboBoxCellEditor(tableViewer.getTable, modes.toArray[String])
      }
      
      override protected def canEdit(element : Object) :Boolean = {
        true
      }
      
      override protected def getValue(element :Object) :Object = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        
        configContext.mode.id.asInstanceOf[Object]
      }
      
      override protected def setValue(element :Object, value :Object) :Unit = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        val mode = value.asInstanceOf[Int]
        
        try {
          configContext.mode = ExecutionMode(mode)
        } catch {
        case e :Throwable => {
          if( configContext.launchConfiguration.supportsMode(ILaunchManager.RUN_MODE)) {
            configContext.mode = ExecutionMode.Run  
          } else if(configContext.launchConfiguration.supportsMode(ILaunchManager.DEBUG_MODE)) {
            configContext.mode = ExecutionMode.Debug
          } else if(configContext.launchConfiguration.supportsMode(ILaunchManager.PROFILE_MODE))
            configContext.mode = ExecutionMode.Profile
          }
        }
        tableViewer.update(element, null)
        updateLaunchConfigurationDialog
        
      }
    }
    
    /** Editing support class for wait termination column
     *
     * @param viewer Table viewer
     */
    class WTEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
      val tableViewer = viewer
      
      override protected def getCellEditor(evement :Object) :CellEditor = {
        new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY)
      }
      override protected def canEdit(element :Object) :Boolean = {
        true
      }
      override protected def getValue(element :Object) :Object = {
        element.asInstanceOf[LaunchConfigurationElement].waitTermination.asInstanceOf[Object]
      }
      override protected def setValue(element :Object, value :Object) :Unit = {
        element.asInstanceOf[LaunchConfigurationElement].waitTermination = value.asInstanceOf[Boolean]
        viewer.update(element, null)
        updateLaunchConfigurationDialog
      }
    }
    
    /** Editing support class for parallel column
     *
     * @param viewer Table viewer
     */
    class ParallelEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
      val tableViewer = viewer
      
      override protected def getCellEditor(evement :Object) :CellEditor = {
        new CheckboxCellEditor(null, SWT.CHECK | SWT.READ_ONLY)
      }
      override protected def canEdit(element :Object) :Boolean = {
        true
      }
      override protected def getValue(element :Object) :Object = {
        element.asInstanceOf[LaunchConfigurationElement].parallel.asInstanceOf[Object]
      }
      override protected def setValue(element :Object, value :Object) :Unit = {
        element.asInstanceOf[LaunchConfigurationElement].parallel = value.asInstanceOf[Boolean]
        viewer.update(element, null)
        updateLaunchConfigurationDialog
      }
    }
    
    /** Editing support class for delay column
     *
     * @param viewer Table viewer
     */
    class DelayEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
      val tableView = viewer
      val editor = new TextCellEditor(viewer.getTable)
      
      override protected def getCellEditor(element :Object) :CellEditor = {
        editor
      }
      
      override protected def canEdit(element : Object) :Boolean = {
        true
      }
      
      override protected def getValue(element :Object) :Object = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.delay.toString
      }
      
      override protected def setValue(element :Object, value :Object) :Unit = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        try {
          var delay = value.asInstanceOf[String].toInt
          if( delay < 0) {
            delay = 0
          }
          configContext.delay = delay
        } catch {
          case e :Throwable => configContext.delay = 0
        }
        tableView.update(element, null)  
        updateLaunchConfigurationDialog
      }
    }
    
    /** Editing support class for execution count column
     *
     * @param viewer Table viewer
     */
    class ExecutionCountEditingSupport(viewer :TableViewer) extends DelayEditingSupport(viewer) {
      override val tableView = viewer
      override val editor = new TextCellEditor(viewer.getTable)
      
      override protected def getValue(element :Object) :Object = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        
        configContext.execCount.toString
      }
      
      override protected def setValue(element :Object, value :Object) :Unit = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        
        try {
          var execCount = value.asInstanceOf[String].toInt
          
          if( execCount < 1) {
            execCount = 1
          }
          configContext.execCount = execCount
        } catch {
          case e :Throwable => configContext.execCount = 1
        }
        tableView.update(element, null)
        updateLaunchConfigurationDialog   
      }
    }
    
    /** Creates plugin's main vision - table with configuratin
     *  
     * @param parent Composite
     */
    def createMainTable(parent :Composite) :Unit = {
      val viewer   = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL )
      val layout   = new TableLayout()
      val gridData = new GridData()
      
      CompositeTabGui.tableViewer = viewer
      gridData.verticalAlignment = GridData.FILL
      gridData.horizontalSpan = 2
      gridData.grabExcessHorizontalSpace = true
      gridData.grabExcessVerticalSpace = true
      gridData.horizontalAlignment = GridData.FILL
      viewer.getControl.setLayoutData(gridData)
      viewer.getTable.setLinesVisible(true)
      viewer.getTable.setHeaderVisible(true)
      
  
      val colNames = Array(
          GuiConstants.tableCol1Name,
          GuiConstants.tableCol2Name,
          GuiConstants.tableCol3Name,
          GuiConstants.tableCol4Name,
          GuiConstants.tableCol5Name,
          GuiConstants.tableCol6Name)
          
      val bounds = Array(
          GuiConstants.TABLE_COL1_WIDTH,
          GuiConstants.TABLE_COL2_WIDTH,
          GuiConstants.TABLE_COL3_WIDTH,
          GuiConstants.TABLE_COL4_WIDTH,
          GuiConstants.TABLE_COL5_WIDTH,
          GuiConstants.TABLE_COL6_WIDTH)
          
      val columnName            = createTableViewerColumn(viewer, colNames(0), bounds(0))
      val columnMode            = createTableViewerColumn(viewer, colNames(1), bounds(1))
      val columnDelay           = createTableViewerColumn(viewer, colNames(2), bounds(2))
      val columnWaitTermination = createTableViewerColumn(viewer, colNames(3), bounds(3))
      val columnExecCount       = createTableViewerColumn(viewer, colNames(4), bounds(4))
      val columnParallel        = createTableViewerColumn(viewer, colNames(5), bounds(5))
      
      columnName.setLabelProvider(new ColumnLabelProvider() {
        override def getText(element :Object) :String = {
          val configContext = element.asInstanceOf[LaunchConfigurationElement]
          configContext.name
        }
      })
      
      columnMode.setLabelProvider(new ColumnLabelProvider() {
        override def getText(element :Object) :String = {
          val configContext = element.asInstanceOf[LaunchConfigurationElement]
          configContext.mode.toString
        }
      })
      columnMode.setEditingSupport(new CompositeTabGui.ModeEditingSupport(viewer))
      
      columnDelay.setLabelProvider(new ColumnLabelProvider() {
        override def getText(element :Object) :String = {
          val configContext = element.asInstanceOf[LaunchConfigurationElement]
          configContext.delay.toString
        }
      })
      
      columnDelay.setEditingSupport(new CompositeTabGui.DelayEditingSupport(viewer))
      columnDelay.getColumn.addSelectionListener(new SelectionListener() {
        override def widgetSelected(event :SelectionEvent) :Unit = {
          
        }
        override def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
      })
      
      columnWaitTermination.setEditingSupport(new CompositeTabGui.WTEditingSupport(viewer))
      columnWaitTermination.setLabelProvider(new ColumnLabelProvider() {
        override def getText(element :Object) :String = {
          val configContext = element.asInstanceOf[LaunchConfigurationElement]
          configContext.waitTermination.toString
        }
      })
      
      columnExecCount.setEditingSupport(new CompositeTabGui.ExecutionCountEditingSupport(viewer))
      columnExecCount.setLabelProvider(new ColumnLabelProvider() {
        override def getText(element :Object) :String = {
          val configContext = element.asInstanceOf[LaunchConfigurationElement]
          configContext.execCount.toString
        }
      })
      
      columnParallel.setEditingSupport(new CompositeTabGui.ParallelEditingSupport(viewer))
      columnParallel.setLabelProvider(new ColumnLabelProvider() {
        override def getText(element :Object) :String = {
          val configContext = element.asInstanceOf[LaunchConfigurationElement]
          configContext.parallel.toString
        }
      })
      
      
      viewer.setContentProvider(new ArrayContentProvider())
      viewer.setInput(configurations)
      CompositeTabGui.tableSelectAction     
    }
    
    /** Creates control buttons on main table
     *  
     * @param parent Composite
     */
    def createMainButtons(parent: Composite) :Unit = {
      val gridDatas = new Array[GridData](5)
      
      for (row <- 0 to 4) {
      gridDatas(row) = new GridData()
      gridDatas(row).horizontalAlignment = GridData.FILL
      gridDatas(row).grabExcessHorizontalSpace = true
      }
  
      
      val buttonAdd    = new Button(parent, SWT.PUSH)
      val buttonRemove = new Button(parent, SWT.PUSH)
      val buttonCopy   = new Button(parent, SWT.PUSH)
      val buttonUp     = new Button(parent, SWT.PUSH)
      val buttonDown   = new Button(parent, SWT.PUSH)
      
      buttonAdd.setText(GuiConstants.buttonAdd)
      buttonRemove.setText(GuiConstants.buttonRemove)
      buttonCopy.setText(GuiConstants.buttonCopy)
      buttonUp.setText(GuiConstants.buttonUp)
      buttonDown.setText(GuiConstants.buttonDown)
      
      buttonAdd.setLayoutData(gridDatas(0))
      buttonRemove.setLayoutData(gridDatas(1))
      buttonCopy.setLayoutData(gridDatas(2))
      buttonUp.setLayoutData(gridDatas(3))
      buttonDown.setLayoutData(gridDatas(4))
      
      CompositeTabGui.buttonAddAction(buttonAdd)
      CompositeTabGui.buttonRemoveAction(buttonRemove)
      CompositeTabGui.buttonCopyAction(buttonCopy)
      CompositeTabGui.buttonUpAction(buttonUp)
      CompositeTabGui.buttonDownAction(buttonDown)
      
      CompositeTabGui.updateButtons
      
    }
    
    /** Creates table columns
     *  
     * @param viewer Table viewer
     * @param title The column's title
     * @param bound The column's bound
     * @return Column viewer
     */
    private def createTableViewerColumn(viewer :TableViewer, title :String, bound :Int) :TableViewerColumn = {
      val viewerColumn = new TableViewerColumn(viewer, SWT.NONE)
      val column = viewerColumn.getColumn
      column.setText(title)
      column.setWidth(bound)
      column.setResizable(true)
      column.setMoveable(true)
      viewerColumn
    }
  }
  
  
  
  override def createControl(parent: Composite) :Unit = {
    val comp = new Composite(parent, SWT.NONE)
    
    comp.setLayout(new GridLayout(1, true))
    comp.setFont(parent.getFont)
    setControl(comp)
    
    CompositeTabGui.createMainTable(comp)
    val buttonComp       = new Composite(comp, SWT.NONE)
    val buttonGridLayout = new GridLayout(5, true)
    
    buttonComp.setLayout(buttonGridLayout)
    buttonComp.setFont(parent.getFont)
    CompositeTabGui.createMainButtons(buttonComp)
    CompositeTabGui.mainComposite = parent  
  }
  
  override def getName() :String = {
    return "Composite"
  }
  
  override def initializeFrom(configuration :ILaunchConfiguration) :Unit = {
    configurationName    = configuration.getName
    configurationType    = configuration.getType
    configurationCurrent = configuration
    
    var storedData :java.util.List[String] = null
    val newConfigurations                  = new Vector[LaunchConfigurationElement]
    
    try {
      storedData = configuration.getAttribute(PluginConstants.STORE_ATTRIBUTE_NAME, new ArrayList[String])
    } catch {
      case e: CoreException => {
        CompositeTabGui.configLoadError
        return
      }
    }
    
    for( element <- storedData.toArray) { 
      newConfigurations.add(new LaunchConfigurationElement(element.asInstanceOf[String]))
    }
    
    configurations = newConfigurations
    ConfigurationHelper.findConfigurations(configurations)
    CompositeTabGui.updateTableData
  }
  
  override def performApply(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {
    configurationCopy.removeAttribute(PluginConstants.STORE_ATTRIBUTE_NAME)
    val storedConfigurations = new ArrayList[String]
    
    for( element <- configurations.toArray) {
      storedConfigurations.add(element.asInstanceOf[LaunchConfigurationElement].serialize)
    }
    configurationCopy.setAttribute(PluginConstants.STORE_ATTRIBUTE_NAME, storedConfigurations)
  }
  
  override def setDefaults(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {
  }
}
