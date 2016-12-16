

package amfipter.plugin.ui

import amfipter.plugin.ui

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions
import scala.io._
//import scala.collection.immutable.

import org.eclipse.debug.ui.AbstractLaunchConfigurationTab
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchConfigurationFilteredTree;
import org.eclipse.debug.internal.ui.launchConfigurations.LaunchGroupFilter;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchGroup;

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
import scala.reflect.io.File
import java.io.PrintWriter
import java.util.ArrayList
import java.util.LinkedList
import java.util.Vector
import org.eclipse.jface.viewers.ISelectionChangedListener
import org.eclipse.jface.viewers.SelectionChangedEvent

//import scala.sys.process.ProcessBuilderImpl.FileOutput


class CompositeTab(lMode :String) extends AbstractLaunchConfigurationTab {
  private val launchMode = lMode
  private val configurations = new Vector[ConfigurationTableContext]//new ArrayBuffer[ConfigurationTableContext]
  
  private class Logger(fileName :String) {
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
    configurations.add(new ConfigurationTableContext("lul " + i.toString()))
  }
  
  private object executionMode extends Enumeration {
    val Run, Debug, Profile = Value
  }
  
  private object GuiSupport {
    var tableViewer :TableViewer = null
    var buttonAdd :Button = null
    var buttonRemove :Button = null
    var buttonCopy :Button = null
    var buttonUp :Button = null
    var buttonDown :Button = null
    
    def buttonAddAction(button :Button) :Unit = {
      buttonAdd = button
      button.addSelectionListener(new SelectionListener() {
        def widgetSelected(event :SelectionEvent) :Unit = {
          updateButtons()
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
            configurations.remove(element.asInstanceOf[ConfigurationTableContext])
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
            val copy = new ConfigurationTableContext(element.asInstanceOf[ConfigurationTableContext])
            val position = configurations.indexOf(element.asInstanceOf[ConfigurationTableContext])
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
        log("add listener")
        def widgetSelected(event :SelectionEvent) :Unit = {
          val selected = tableViewer.getStructuredSelection()
          log.println("PRESS")
//          log.println(selectedElements.toString())
//          log.println(selectedElements.asInstanceOf[ConfigurationTableContext].toString())
//          for(i <- selectedElements) {
//            log(i.getData())
//            log(configurations.indexOf(i.getData()))
//          }
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
        log("remove listener")
        def widgetSelected(event :SelectionEvent) :Unit = {
//          val table = tableViewer.getTable() 
//          val selectedElements = table.getSelection()
          val selected = tableViewer.getStructuredSelection()
          log.println("PRESS")
//          log.println(selectedElements.toString())
//          for(i <- selectedElements) {
//            log(i.getData())
//            log(configurations.indexOf(i.getData()))
//          }
          for( configuration <- selected.toArray() if configurations.indexOf(configuration) >= configurations.size() - 1) {
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
      } else {
        buttonUp.setEnabled(true)
      }
      if( selected.size() == 1 && configurations.indexOf(selected.getFirstElement()) == configurations.size() - 1) {
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
  }
  
  private class ConfigurationTableContext {
    var name = "test"
    var mode = executionMode.Run
    var execCount = 1
    var waitTermination = false
    var delay = 0
    
    def this(customName :String)  {
      this()
      name = customName
    }
    
    def this(another :ConfigurationTableContext) {
      this()
      name = another.name
      mode = another.mode 
      execCount = another.execCount
      waitTermination = another.waitTermination
      delay = another.delay
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
      val configContext = element.asInstanceOf[ConfigurationTableContext]
      configContext.mode.id.asInstanceOf[Object]
//      return 1.asInstanceOf[Object]
      2.asInstanceOf[Object]
    }
    
    override protected def setValue(element :Object, value :Object) :Unit = {
      val configContext = element.asInstanceOf[ConfigurationTableContext]
      val mode = value.asInstanceOf[Int]
      configContext.mode = executionMode(mode)
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
      val configContext = element.asInstanceOf[ConfigurationTableContext]
      configContext.delay.asInstanceOf[Object]
    }
    
    override protected def setValue(element :Object, value :Object) :Unit = {
      val configContext = element.asInstanceOf[ConfigurationTableContext]
//      println(value.toString())
//      val mode = value.asInstanceOf[String].toInt
      configContext.mode = executionMode(0)
      tableView.update(element, null)
      
    }
  }
  
  private class ExecutionCountEditingSupport(viewer :TableViewer) extends DelayEditingSupport(viewer) {
    override val tableView = viewer
    override val editor = new TextCellEditor(viewer.getTable())
//    editor.setValidator(new NumberValidator())
    
    override protected def getCellEditor(element :Object) :CellEditor = {
      return editor
    }
    
    override protected def canEdit(element : Object) :Boolean = {
      true
    }
    
    override protected def getValue(element :Object) :Object = {
      val configContext = element.asInstanceOf[ConfigurationTableContext]
      configContext.execCount.asInstanceOf[Object]
    }
    
    override protected def setValue(element :Object, value :Object) :Unit = {
      val configContext = element.asInstanceOf[ConfigurationTableContext]
      val count = value.asInstanceOf[Int]
      configContext.execCount = count
      tableView.update(element, null)
      
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
        val configContext = element.asInstanceOf[ConfigurationTableContext]
        configContext.name
      }
    })
    
    col2.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[ConfigurationTableContext]
        configContext.mode.toString()
      }
    })
    col2.setEditingSupport(new ModeEditingSupport(viewer))
    
    col3.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[ConfigurationTableContext]
        configContext.delay.toString()
      }
    })
    
    col3.setEditingSupport(new DelayEditingSupport(viewer))
    
    col4.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[ConfigurationTableContext]
        configContext.waitTermination.toString()
      }
    })
    
    col5.setLabelProvider(new ColumnLabelProvider() {
      override def getText(element :Object) :String = {
        val configContext = element.asInstanceOf[ConfigurationTableContext]
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
    
  }
  
  override def performApply(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {
    
  }
  
  override def setDefaults(configurationCopy :ILaunchConfigurationWorkingCopy) :Unit = {

    
  }
  
  
}