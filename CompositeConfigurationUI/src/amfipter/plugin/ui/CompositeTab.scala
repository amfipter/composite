

package amfipter.plugin.ui

import amfipter.plugin.ui
import amfipter.plugin.LaunchConfigurationElement
import amfipter.plugin.ExecutionMode
import amfipter.plugin.CompositeConfiguration


import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions
import scala.io._
import scala.util.control.Breaks._
import scala.util.Random
//import scala.collection.immutable.

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationFilteredTree
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupFilter;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.debug.internal.core.LaunchConfiguration

import org.eclipse.debug.core.ILaunchConfiguration
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.events.SelectionListener
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.WorkbenchActivityHelper

import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
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
import org.eclipse.jface.viewers.ComboBoxCellEditor;
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

//import scala.sys.process.ProcessBuilderImpl.FileOutput



class CompositeTab(lMode :String) extends AbstractLaunchConfigurationTab {
  private val launchMode = lMode
  private var configurations = new Vector[LaunchConfigurationElement]//new ArrayBuffer[ConfigurationTableContext]
  private var configurationName = ""
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
  for( i <- 0 to 3) {
    configurations.add(new LaunchConfigurationElement())
  }
 
  private object GuiSupport {
    var tableViewer :TableViewer = null
    private var buttonAdd :Button = null
    private var buttonRemove :Button = null
    private var buttonCopy :Button = null
    private var buttonUp :Button = null
    private var buttonDown :Button = null
    
    var mainComposite :Composite = null
    private var selectedConfigurations :ITreeSelection = null
    
    private class AddDialog(parentShell :Shell, parentMode :String) extends Dialog(parentShell) {
      val manager = DebugUIPlugin.getDefault.getLaunchConfigurationManager
      val launchGroups = manager.getLaunchGroups
      
      
      
      val mode = parentMode
      val filter = new ViewerFilter() {
        override def select(viewer :Viewer, parentElement :Object, element :Object) :Boolean = {
          
          if( element.isInstanceOf[ILaunchConfigurationType]) {
            return getLaunchManager.getLaunchConfigurations(element.asInstanceOf[ILaunchConfigurationType]).length > 0
          } else if( element.isInstanceOf[ILaunchConfiguration]) {
            //need check this
            return DebugUIPlugin.doLaunchConfigurationFiltering(element.asInstanceOf[ILaunchConfiguration]) && 
              !WorkbenchActivityHelper.filterItem(element.asInstanceOf[ILaunchConfiguration]) &&
              !configurationName.equals(element.asInstanceOf[ILaunchConfiguration].getName)
          } else 
            return false
        }
      }
      
      override protected def createDialogArea(parent :Composite) :Control = {
        val container = super.createDialogArea(parent).asInstanceOf[Composite]
        log(launchGroups)
//        val launchGroup = 
        val lTree = new LaunchConfigurationFilteredTree(parent, 
            SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, 
            new PatternFilter(), launchGroups(0), null)
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
              log("Added")
              log(launchElement)
              log(launchElement.launchConfiguration)
              
              ConfigurationHelper.initId(configuration.asInstanceOf[ILaunchConfiguration])
              launchElement.id = configuration.asInstanceOf[ILaunchConfiguration].getAttribute(GuiConstants.storeIdPrefix, "")
              
              configurations.add(launchElement)
            }
          }
          updateButtons()
          tableViewer.refresh()
          updateLaunchConfigurationDialog()
        }
        def widgetDefaultSelected(event :SelectionEvent) :Unit ={}
      })
    }
    
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
    
    def buttonDownAction(button :Button) :Unit = {
      buttonDown = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection()
          log.println("PRESS DOWN")
          for( configuration <- selected.toArray() if configurations.indexOf(configuration) < configurations.size() - 1) {
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
    
    def tableSelectAction() :Unit = {
      tableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
        def selectionChanged(event :SelectionChangedEvent) :Unit = {
          updateButtons()
        }
      })
    }
    
    def updateTableData() :Unit = {
      tableViewer.setInput(configurations)
    }
  }
   
  private class NumberValidator extends ICellEditorValidator {
    override def isValid(element :Object) :String = {
      val string1 = element.asInstanceOf[String]
      try {
        val t = augmentString(string1).toInt
      } catch {
        case e: Exception => return "Number only"
      }
      null
    }
  }
  
  private class ModeEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
    val tableViewer = viewer//.asInstanceOf[TableViewer]
    
    override protected def getCellEditor(element :Object) :CellEditor = {
      val modes = Array("Run", "Debug", "Profile")
      new ComboBoxCellEditor(tableViewer.getTable(), modes)
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
      configContext.mode = ExecutionMode(mode)
//      LaunchConfigurationElement
      tableViewer.update(element, null)
      
    }
  }
  
  private class DelayEditingSupport(viewer :TableViewer) extends EditingSupport(viewer) {
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
    }
  }
  
  private class ExecutionCountEditingSupport(viewer :TableViewer) extends DelayEditingSupport(viewer) {
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
      
    }
  }
    
  private object ConfigurationHelper {
    
    def initId(launchConfiguration : ILaunchConfiguration) :Unit = {
      val id = launchConfiguration.getAttribute(GuiConstants.storeIdPrefix, "")
      if( id.equals("")) {
        val wc = launchConfiguration.getWorkingCopy
        wc.setAttribute(GuiConstants.storeIdPrefix, getNewId)
        wc.doSave
      }
    }
    
    def findConfigurations() :Unit = {
      val launchConfugurations = DebugPlugin.getDefault.getLaunchManager.getLaunchConfigurations
      for( launchConfuguration <- launchConfugurations) {
        val id = launchConfuguration.getAttribute(GuiConstants.storeIdPrefix, "")
        for(configurationIndex <- 0 until configurations.size) {
          if( configurations.get(configurationIndex).id.equals(id)) {
            configurations.get(configurationIndex).launchConfiguration = launchConfuguration
            configurations.get(configurationIndex).name = launchConfuguration.getName
//            break
          }
        }  
      }
    }
    def getNewId() :String = {
      val usedId = new ArrayBuffer[String]
      configurations.toArray().map(x => usedId += x.asInstanceOf[LaunchConfigurationElement].id)
      val random = new Random
      var newId = random.alphanumeric.take(GuiConstants.configurationIdStringSize).mkString
      while(usedId.contains(newId)) {
        newId = random.alphanumeric.take(GuiConstants.configurationIdStringSize).mkString
      }
      newId
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
        GuiConstants.tableCol5Name)
        
    val bounds = Array(
        GuiConstants.tableCol1Width,
        GuiConstants.tableCol2Width,
        GuiConstants.tableCol3Width,
        GuiConstants.tableCol4Width,
        GuiConstants.tableCol5Width)
        
//    for( i <- 0 to 4) {
//      val col = createTableViewerColumn(viewer, colNames(i), bounds(i))
//    }
    val col1 = createTableViewerColumn(viewer, colNames(0), bounds(0))
    val col2 = createTableViewerColumn(viewer, colNames(1), bounds(1))
    val col3 = createTableViewerColumn(viewer, colNames(2), bounds(2))
    val col4 = createTableViewerColumn(viewer, colNames(3), bounds(3))
    val col5 = createTableViewerColumn(viewer, colNames(4), bounds(4))
    
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
    col2.setEditingSupport(new ModeEditingSupport(viewer))
//    col2.getViewer.setCellModifier(modifier)
    
    col3.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.delay.toString()
      }
    })
    
    col3.setEditingSupport(new DelayEditingSupport(viewer))
    col3.getColumn.addSelectionListener(new SelectionListener() {
      override def widgetSelected(event :SelectionEvent) :Unit = {
        
      }
      override def widgetDefaultSelected(event :SelectionEvent) :Unit = {}
    })
    
    col4.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.waitTermination.toString()
      }
    })
    
    col5.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[LaunchConfigurationElement]
        configContext.execCount.toString()
      }
    })
    
    col5.setEditingSupport(new ExecutionCountEditingSupport(viewer))
    
    viewer.setContentProvider(new ArrayContentProvider())
    viewer.setInput(configurations)
    GuiSupport.tableSelectAction()
    
  }
  
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
    val tempList = new ArrayList[String]
    val newConfigurations = new Vector[LaunchConfigurationElement]
    val storedData = configuration.getAttribute(GuiConstants.storeAttributeName, tempList)
    for( element <- storedData.toArray()) {
      log(element)
      newConfigurations.add(new LaunchConfigurationElement(element.asInstanceOf[String]))
    }
    configurations = newConfigurations
    ConfigurationHelper.findConfigurations()
    GuiSupport.updateTableData()
    log("=====================")
  }
  
  override def performApply(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {
    log("---performApply---")
    configurationCopy.removeAttribute(GuiConstants.storeAttributeName)
    val tempList = new ArrayList[String]
    log(configurations)
    for( element <- configurations.toArray()) {
      tempList.add(element.asInstanceOf[LaunchConfigurationElement].serialize())
    }
    configurationCopy.setAttribute(GuiConstants.storeAttributeName, tempList)
    log("=====================")
  
//    configurationCopy.setAttribute("configurations", configurations.toArray())
  }
  
  override def setDefaults(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {

    
  }
}
