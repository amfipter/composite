

package amfipter.plugin.ui

import amfipter.plugin.ui
import amfipter.plugin.LaunchConfigurationElement
import amfipter.plugin.ExecutionMode
import amfipter.plugin.CompositeConfiguration
import amfipter.plugin.PluginConstants
import amfipter.plugin.CompositePluginException
import amfipter.plugin.ConfigurationHelper


import scala.collection.mutable.ArrayBuffer
//import scala.collection.JavaConversions._
import scala.io._
import scala.util.control.Breaks._
import scala.util.Random
import scala.collection.immutable.List

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab
import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationFilteredTree
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupFilter
import org.eclipse.debug.ui.DebugUITools
import org.eclipse.debug.ui.ILaunchGroup
import org.eclipse.debug.internal.core.LaunchConfiguration

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter
import org.eclipse.swt.accessibility.AccessibleEvent
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.eclipse.swt.events.SelectionListener
import org.eclipse.ui.PlatformUI
import org.eclipse.ui.activities.WorkbenchActivityHelper

import org.eclipse.debug.internal.ui.IDebugHelpContextIds
import org.eclipse.swt.widgets.Display
import org.eclipse.ui.dialogs.PatternFilter
import org.eclipse.jface.viewers.TableLayout
import org.eclipse.jface.viewers.ColumnWeightData
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.TableItem
import org.eclipse.jface.viewers.TableViewer
import org.eclipse.jface.viewers.TableViewerColumn
import org.eclipse.jface.viewers.ArrayContentProvider
import org.eclipse.jface.viewers.ColumnLabelProvider
import org.eclipse.jface.viewers.EditingSupport
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ComboBoxCellEditor
import org.eclipse.jface.viewers.TextCellEditor
import org.eclipse.jface.viewers.ICellEditorValidator
import org.eclipse.jface.viewers.ColumnViewer
import org.eclipse.jface.viewers.ViewerFilter
import org.eclipse.jface.viewers.Viewer
import org.eclipse.jface.dialogs.Dialog
//import scala.reflect.io.File
import java.io.PrintWriter
import java.util.ArrayList
import java.util.LinkedList
import java.util.Vector
import org.eclipse.jface.viewers.ISelectionChangedListener
import org.eclipse.jface.viewers.SelectionChangedEvent
import java.io.ObjectOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.ObjectInputStream
import org.eclipse.debug.internal.ui.DebugUIPlugin
import org.eclipse.debug.core.ILaunchConfigurationType
import org.eclipse.jface.viewers.ITreeSelection
import java.awt.Window
import amfipter.plugin.LaunchConfigurationElement
import org.eclipse.debug.core.DebugPlugin
import org.eclipse.jface.viewers.CheckboxCellEditor
import org.eclipse.core.runtime.Status
import org.eclipse.core.runtime.IStatus
import org.eclipse.jface.dialogs.ErrorDialog
import org.eclipse.core.runtime.CoreException
import org.eclipse.debug.core.ILaunchManager

//import scala.sys.process.ProcessBuilderImpl.FileOutput


/** Composite configuration GUI
 *
 */
class CompositeTab(lMode :String) extends AbstractLaunchConfigurationTab {
  private val launchMode = lMode
  private var configurations = new Vector[LaunchConfigurationElement]//new ArrayBuffer[ConfigurationTableContext]
  private var configurationName = ""
  private var configurationType :ILaunchConfigurationType = null
  private var configurationCurrent :ILaunchConfiguration = null 
//  val test = new CompositeConfiguration
//  val t = new LaunchConfiguration()
  
//  this.
  
  
  

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
  
  private val log = new Logger("LOG")
 
  /** Support GUI class
   * 
   * Provides buttons behavior, update table information 
   */
  private object GuiSupport {
    var tableViewer :TableViewer = null
    private var buttonAdd :Button = null
    private var buttonRemove :Button = null
    private var buttonCopy :Button = null
    private var buttonUp :Button = null
    private var buttonDown :Button = null
    
    var mainComposite :Composite = null
    private var selectedConfigurations :ITreeSelection = null
    
    /** Dialog class that provides adding new launch configuration
     *  
     * @param parentShell Application shell
     * @param parentMode launch mode (etc. run/debug..)
     */
    private class AddDialog(parentShell :Shell, parentMode :String) extends Dialog(parentShell) {
      val manager = DebugUIPlugin.getDefault.getLaunchConfigurationManager
      val launchGroups = manager.getLaunchGroups
      val mode = parentMode
      val launchGroup = getLaunchGroup(mode)
      
      
      val filter = new ViewerFilter() {
        override def select(viewer :Viewer, parentElement :Object, element :Object) :Boolean = {
//          return true
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
        for( i <- launchGroups) {
          log(i.getIdentifier)
        }
//        val launchGroup = 
        
        val lTree = new LaunchConfigurationFilteredTree(parent, 
            SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, 
            new PatternFilter(), launchGroup, null)
        lTree.createViewControl
        val filters = lTree.getViewer().getFilters()
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
        new Point(GuiConstants.dialogHeight, GuiConstants.dialogWigth)
      }
    }
    

    /** Creates dialog to adding new launch configuration 
     * 
     * @param button - Target button
     */
    def buttonAddAction(button :Button) :Unit = {
      buttonAdd = button
      button.addSelectionListener(new SelectionListener() {
        
        def widgetSelected(event :SelectionEvent) :Unit = {
          log("PRESS ADD")
          val dialog = new AddDialog(mainComposite.getShell(), launchMode)
          dialog.create()
          
          // scala can't find constants Dialog.OK or Window.OK
          if( dialog.open() == GuiConstants.dialogOK) {                
            for( configuration <- selectedConfigurations.toArray() if configuration.isInstanceOf[ILaunchConfiguration]) {
              val launchElement = new LaunchConfigurationElement
              launchElement.name = configuration.asInstanceOf[ILaunchConfiguration].getName
              launchElement.launchConfiguration = configuration.asInstanceOf[ILaunchConfiguration]
              log(configuration.asInstanceOf[ILaunchConfiguration].getModes())
              log("Added")
              log(launchElement)
              log(launchElement.launchConfiguration)
              
              ConfigurationHelper.initId(configuration.asInstanceOf[ILaunchConfiguration], configurations.toArray)
              launchElement.id = configuration.asInstanceOf[ILaunchConfiguration].getAttribute(PluginConstants.storeIdPrefix, "")
              var cycle :(Boolean, Array[String]) = null
              configurations.add(launchElement)
              try {
                cycle = ConfigurationHelper.findCycle(configurationCurrent, configurationType, configurations.toArray)
              } catch {
                case e :CompositePluginException => {
                  //plugin can't add new configuration
                  log("ADD ERROR")
                  log(e.getMessage)
                  configurations.remove(launchElement)
                  return
                }
              }
              
              log(cycle)
              if(cycle._1.equals(true)) {
                val cycleElements = new StringBuilder
                for( element <- cycle._2) {
                  cycleElements ++= element
                  cycleElements ++= " -> \n"
                }
                cycleElements ++= configurationCurrent.getName
                log(cycleElements.mkString)
                val status = new Status(IStatus.ERROR, "amfipter.plugin.ui", cycleElements.mkString)
                ErrorDialog.openError(mainComposite.getShell, GuiConstants.cycleError, GuiConstants.cycleErrorDescription, status)
                configurations.remove(launchElement)
              }
            }
          }
          updateButtons()
          tableViewer.refresh()
          updateLaunchConfigurationDialog() 
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit ={}
      })
    } 
    
    /** Remove selected table's elements
     * 
     * @param button - Target button
     */
    def buttonRemoveAction(button :Button) :Unit = {
      buttonRemove = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection()
          for(element <- selected.toArray()) {
//            log(element)
//            configurations.remove(element)
            configurations.remove(element.asInstanceOf[LaunchConfigurationElement])
          }
          val t = true
          tableViewer.refresh()  
          updateButtons()
          updateLaunchConfigurationDialog()
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit ={}
      })
    }
    
    /** Copy selected table's elements
     * 
     * @param button - Target button
     */
    def buttonCopyAction(button :Button) :Unit = {
      buttonCopy = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection()
          for(element <- selected.toArray()) {
            val copy = new LaunchConfigurationElement(element.asInstanceOf[LaunchConfigurationElement])
            val position = configurations.indexOf(element.asInstanceOf[LaunchConfigurationElement])
            configurations.insertElementAt(copy, position)
          }
          tableViewer.refresh()
          updateButtons()
          updateLaunchConfigurationDialog()
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
      })
      
    }
    
    /** Shift selected table's elements up
     * 
     * @param button - Target button
     */
    def buttonUpAction(button :Button) :Unit = {
      buttonUp = button
      button.addSelectionListener(new SelectionListener() {
//        log("add listener")
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection()
          log.println("PRESS")
          for( configuration <- selected.toArray() if configurations.indexOf(configuration) > 0) {
            val position = configurations.indexOf(configuration)
            val element = configurations.get(position)
            configurations.remove(position)
            configurations.insertElementAt(element, position - 1)
          }
          tableViewer.refresh()
          updateButtons()
          updateLaunchConfigurationDialog()
          
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
      })
      
    }
    
    /** Shift selected table's elements down
     * 
     * @param button - Target button
     */
    def buttonDownAction(button :Button) :Unit = {
      buttonDown = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection()
          log.println("PRESS DOWN")
          for( configuration <- selected.toArray.reverse if configurations.indexOf(configuration) < configurations.size() - 1) {
            val position = configurations.indexOf(configuration)
            val element =  configurations.get(position)
            configurations.remove(position)
            configurations.insertElementAt(element, position + 1)
          }
          tableViewer.refresh()
          updateButtons()
          updateLaunchConfigurationDialog()
        }
        
        def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
       })
    }
    
    /** Update buttons activity
     * 
     */
    def updateButtons() :Unit = {
      val selected = tableViewer.getStructuredSelection()
//      if (selected.size() > 0)  buttonRemove.setEnabled(true) else buttonRemove.setEnabled(false)
      if (selected.size() == 0) {
        buttonRemove.setEnabled(false)
        buttonCopy.setEnabled(false)
        buttonUp.setEnabled(false) 
        buttonDown.setEnabled(false)
        return
      }
      buttonRemove.setEnabled(true)
      buttonCopy.setEnabled(true)
      if( selected.size() == 1 && configurations.indexOf(selected.getFirstElement()) == 0) {
        buttonUp.setEnabled(false)
      } else if( selected.size() > 1 && !selected.toArray.filter(x => configurations.indexOf(x) == 0).isEmpty) {
        buttonUp.setEnabled(false)
      } else {
        buttonUp.setEnabled(true)
      }
      if( selected.size() == 1 && configurations.indexOf(selected.getFirstElement()) == configurations.size() - 1) {
        buttonDown.setEnabled(false)
      } else if( selected.size() > 1 && !selected.toArray.filter(x => configurations.indexOf(x) == configurations.size - 1).isEmpty) {
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
          updateButtons()
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
      val tableViewer = viewer//.asInstanceOf[TableViewer]
      
      override protected def getCellEditor(element :Object) :CellEditor = {
        val modes = new ArrayBuffer[String]
        val configuration = element.asInstanceOf[LaunchConfigurationElement].launchConfiguration
        
        if( configuration.supportsMode(ILaunchManager.RUN_MODE)) {
          modes += "Run"
        }
        if( configuration.supportsMode(ILaunchManager.DEBUG_MODE)) {
          modes += "Debug"
        }
        if( configuration.supportsMode(ILaunchManager.PROFILE_MODE)) {
          modes += "Profile"
        }
        new ComboBoxCellEditor(tableViewer.getTable(), modes.toArray[String])
      }
      
      override protected def canEdit(element : Object) :Boolean = {
        true
      }
      
      override protected def getValue(element :Object) :Object = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.mode.id.asInstanceOf[Object]
  //      return 1.asInstanceOf[Object]
        2.asInstanceOf[Object]
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
        updateLaunchConfigurationDialog()
        
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
        updateLaunchConfigurationDialog()
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
        updateLaunchConfigurationDialog()
      }
    }
    
    /** Editing support class for delay column
     *
     * @param viewer Table viewer
     */
    class DelayEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
      val tableView = viewer
      val editor = new TextCellEditor(viewer.getTable())
  //    editor.setValidator(new NumberValidator())
      
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
        updateLaunchConfigurationDialog()
      }
    }
    
    /** Editing support class for execution count column
     *
     * @param viewer Table viewer
     */
    class ExecutionCountEditingSupport(viewer :TableViewer) extends DelayEditingSupport(viewer) {
      override val tableView = viewer
      override val editor = new TextCellEditor(viewer.getTable())
      
      override protected def getCellEditor(element :Object) :CellEditor = {
        return editor
      }
      
      override protected def canEdit(element : Object) :Boolean = {
        true
      }
      
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
        updateLaunchConfigurationDialog()   
      }
    }
  }
  
  
  
  
  
  
  
  
  
  
  
  override def createControl(parent: Composite) :Unit = {
    val comp = new Composite(parent, SWT.NONE)
    comp.setLayout(new GridLayout(1, true))
    comp.setFont(parent.getFont())
    setControl(comp)
    
    val launchGroup = getLaunchGroup(launchMode)
    createMainTable(comp)
    val buttonComp = new Composite(comp, SWT.NONE)
    val buttonGridLayout = new GridLayout(5, true)
    //buttonGridLayout.
    buttonComp.setLayout(new GridLayout(5, true))
    buttonComp.setFont(parent.getFont())
    createMainButtons(buttonComp)
    GuiSupport.mainComposite = parent
    
    
  }
  
  /** Creates plugin's main vision - table with configuratin
   *  
   * @param parent composite
   */
  private def createMainTable(parent :Composite) :Unit = {
    val viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL )
    GuiSupport.tableViewer = viewer
    val layout = new TableLayout()
    val gridData = new GridData()
    gridData.verticalAlignment = GridData.FILL
    gridData.horizontalSpan = 2
    gridData.grabExcessHorizontalSpace = true
    gridData.grabExcessVerticalSpace = true
    gridData.horizontalAlignment = GridData.FILL
    viewer.getControl().setLayoutData(gridData)
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
        GuiConstants.tableCol1Width,
        GuiConstants.tableCol2Width,
        GuiConstants.tableCol3Width,
        GuiConstants.tableCol4Width,
        GuiConstants.tableCol5Width,
        GuiConstants.tableCol6Width)
        
//    for( i <- 0 to 4) {
//      val col = createTableViewerColumn(viewer, colNames(i), bounds(i))
//    }
    val col1 = createTableViewerColumn(viewer, colNames(0), bounds(0))
    val col2 = createTableViewerColumn(viewer, colNames(1), bounds(1))
    val col3 = createTableViewerColumn(viewer, colNames(2), bounds(2))
    val col4 = createTableViewerColumn(viewer, colNames(3), bounds(3))
    val col5 = createTableViewerColumn(viewer, colNames(4), bounds(4))
    val col6 = createTableViewerColumn(viewer, colNames(5), bounds(5))
    
    col1.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.name
      }
    })
    
    col2.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.mode.toString()
      }
    })
    col2.setEditingSupport(new GuiSupport.ModeEditingSupport(viewer))
//    col2.getViewer.setCellModifier(modifier)
    
    col3.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.delay.toString()
      }
    })
    
    col3.setEditingSupport(new GuiSupport.DelayEditingSupport(viewer))
    col3.getColumn.addSelectionListener(new SelectionListener() {
      override def widgetSelected(event :SelectionEvent) :Unit = {
        
      }
      override def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
    })
    
    col4.setEditingSupport(new GuiSupport.WTEditingSupport(viewer))
    col4.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.waitTermination.toString()
      }
    })
    
    col5.setEditingSupport(new GuiSupport.ExecutionCountEditingSupport(viewer))
    col5.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.execCount.toString()
      }
    })
    
    col6.setEditingSupport(new GuiSupport.ParallelEditingSupport(viewer))
    col6.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.parallel.toString()
      }
    })
    
    
    viewer.setContentProvider(new ArrayContentProvider())
    viewer.setInput(configurations)
    GuiSupport.tableSelectAction()
    
  }
  
  /** Creates control buttons on main table
   *  
   * @param parent composite
   */
  private def createMainButtons(parent: Composite) :Unit = {
    val gridDatas = new Array[GridData](5)
    for (i <- 0 to 4) {
    gridDatas(i) = new GridData()
    gridDatas(i).horizontalAlignment = GridData.FILL
    gridDatas(i).grabExcessHorizontalSpace = true
    }

    
    val buttonAdd = new Button(parent, SWT.PUSH)
    val buttonRemove = new Button(parent, SWT.PUSH)
    val buttonCopy = new Button(parent, SWT.PUSH)
    val buttonUp = new Button(parent, SWT.PUSH)
    val buttonDown = new Button(parent, SWT.PUSH)
    
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
    
    GuiSupport.buttonAddAction(buttonAdd)
    GuiSupport.buttonRemoveAction(buttonRemove)
    GuiSupport.buttonCopyAction(buttonCopy)
    GuiSupport.buttonUpAction(buttonUp)
    GuiSupport.buttonDownAction(buttonDown)
    
    GuiSupport.updateButtons()
    
  }
  
  /** Creates table columns
   *  
   * @param viewer table viewer
   * @param title the column's title
   * @param bound the column's bound
   * @return column viewer
   */
  private def createTableViewerColumn(viewer :TableViewer, title :String, bound :Int) :TableViewerColumn = {
    val viewerColumn = new TableViewerColumn(viewer, SWT.NONE)
    val column = viewerColumn.getColumn()
    column.setText(title)
    column.setWidth(bound)
    column.setResizable(true)
    column.setMoveable(true)
    viewerColumn
  }
  
  def getLaunchGroup(launchMode :String) :ILaunchGroup = {
    val lGroups = DebugUITools.getLaunchGroups().toArray[ILaunchGroup]
    lGroups.filter(_.getMode().equals(launchMode))(0)
  }
  
  override def getName() :String = {
    return "Composite"
  }
  override def initializeFrom(configuration :ILaunchConfiguration) :Unit = {
    log("---initializeFrom---")
    configurationName = configuration.getName
    configurationType = configuration.getType
    configurationCurrent = configuration
    var storedData :java.util.List[String] = null
    
    val newConfigurations = new Vector[LaunchConfigurationElement]
    try {
      storedData = configuration.getAttribute(PluginConstants.storeAttributeName, new ArrayList[String])
    } catch {
      case e: CoreException => {
        GuiSupport.configLoadError()
        return
      }
    }
    for( element <- storedData.toArray) { 
      log(element)
      newConfigurations.add(new LaunchConfigurationElement(element.asInstanceOf[String]))
    }
    configurations = newConfigurations
    ConfigurationHelper.findConfigurations(configurations)
    GuiSupport.updateTableData
    log("=====================")
  }
  
  override def performApply(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {
    log("---performApply---")
    configurationCopy.removeAttribute(PluginConstants.storeAttributeName)
    val tempList = new ArrayList[String]
    log(configurations)
    for( element <- configurations.toArray()) {
      tempList.add(element.asInstanceOf[LaunchConfigurationElement].serialize())
    }
    configurationCopy.setAttribute(PluginConstants.storeAttributeName, tempList)
    log("=====================")
  
//    configurationCopy.setAttribute("configurations", configurations.toArray())
  }
  
  override def setDefaults(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {
  }
}
