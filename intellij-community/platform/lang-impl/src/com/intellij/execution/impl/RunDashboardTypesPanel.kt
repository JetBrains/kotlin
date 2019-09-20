// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.impl

import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.dashboard.RunDashboardManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.speedSearch.SpeedSearchUtil
import com.intellij.util.containers.ContainerUtil
import gnu.trove.THashSet
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.util.function.Consumer
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

@NonNls
private val EXPAND_PROPERTY_KEY = "ExpandRunDashboardTypesPanel"

private val IGNORE_CASE_DISPLAY_NAME_COMPARATOR = Comparator<ConfigurationType> { t1, t2 ->
  t1.displayName.compareTo(t2.displayName, ignoreCase = true)
}

/**
 * @author Konstantin Aleev
 */
internal class RunDashboardTypesPanel(private val myProject: Project) : JPanel(BorderLayout()) {
  private val listModel = CollectionListModel<ConfigurationType>()
  private val list = JBList<ConfigurationType>(listModel)

  init {
    val search = ListSpeedSearch(list) { it.displayName }
    search.comparator = SpeedSearchComparator(false)
    list.visibleRowCount = 5

    list.cellRenderer = object : ColoredListCellRenderer<ConfigurationType>() {
      override fun customizeCellRenderer(list: JList<out ConfigurationType>,
                                         value: ConfigurationType?,
                                         index: Int,
                                         selected: Boolean,
                                         hasFocus: Boolean) {
        if (value != null) {
          append(value.displayName)
          icon = value.icon
        }

        SpeedSearchUtil.applySpeedSearchHighlighting(list, this, true, selected)
      }
    }

    val toolbarDecorator = ToolbarDecorator.createDecorator(list)
    if (!SystemInfo.isMac) {
      toolbarDecorator.setAsUsualTopToolbar()
    }
    toolbarDecorator.setAddAction { button ->
      showAddPopup(myProject, listModel.items.map(ConfigurationType::getId).toSet(),
                   Consumer { newTypes ->
                     newTypes.forEach { listModel.add(it) }
                     listModel.sort(IGNORE_CASE_DISPLAY_NAME_COMPARATOR)
                     list.selectedIndices = newTypes.map { listModel.getElementIndex(it) }.filter { it != -1 }.toIntArray()
                     val selectedIndex = list.selectedIndex
                     val cellBounds = list.getCellBounds(selectedIndex, selectedIndex)
                     if (cellBounds != null) {
                       list.scrollRectToVisible(cellBounds)
                     }
                   },
                   Consumer { it.show(button.preferredPopupPoint!!) })
    }
    toolbarDecorator.setRemoveAction { list.selectedValuesList.forEach { listModel.remove(it) } }
    toolbarDecorator.setMoveUpAction(null)
    toolbarDecorator.setMoveDownAction(null)

    val listPanel = JPanel(BorderLayout())
    listPanel.add(toolbarDecorator.createPanel(), BorderLayout.CENTER)

    val hideableDecorator = object : HideableDecorator(this,
                                                       ExecutionBundle.message("run.dashboard.configurable.types.panel.title",
                                                                               RunDashboardManager.getInstance(myProject).toolWindowId),
                                                       false) {
      override fun on() {
        super.on()
        storeState()
      }

      override fun off() {
        super.off()
        storeState()
      }

      private fun storeState() {
        PropertiesComponent.getInstance().setValue(EXPAND_PROPERTY_KEY, isExpanded.toString())
      }
    }
    hideableDecorator.setOn(PropertiesComponent.getInstance().getBoolean(EXPAND_PROPERTY_KEY, false))
    hideableDecorator.setContentComponent(listPanel)
  }

  fun addChangeListener(onChange: () -> Unit) {
    listModel.addListDataListener(object : ListDataListener {
      override fun contentsChanged(e: ListDataEvent?) {
        onChange()
      }

      override fun intervalRemoved(e: ListDataEvent?) {
        onChange()
      }

      override fun intervalAdded(e: ListDataEvent?) {
        onChange()
      }
    })
  }

  fun isModified() = listModel.items.mapTo(THashSet()) { it.id } != RunDashboardManager.getInstance(myProject).types

  fun reset() {
    listModel.removeAll()
    val types = RunDashboardManager.getInstance(myProject).types
    listModel.add(ConfigurationType.CONFIGURATION_TYPE_EP.extensionList.filter { types.contains(it.id) })
    listModel.sort(IGNORE_CASE_DISPLAY_NAME_COMPARATOR)
  }

  fun apply() {
    val dashboardManager = RunDashboardManager.getInstance(myProject)
    val types = listModel.items.mapTo(THashSet()) { it.id }
    if (types != dashboardManager.types) {
      dashboardManager.types = types
    }
  }

  companion object {
    @JvmStatic
    fun showAddPopup(project: Project, addedTypes: Set<String>,
                     onAddCallback: Consumer<List<ConfigurationType>>, popupOpener: Consumer<JBPopup>) {
      showAddPopup(project, addedTypes, onAddCallback, popupOpener, true)
    }

    private fun showAddPopup(project: Project, addedTypes: Set<String>,
                             onAddCallback: Consumer<List<ConfigurationType>>, popupOpener: Consumer<JBPopup>,
                             showApplicableTypesOnly: Boolean) {
      val allTypes = ConfigurationType.CONFIGURATION_TYPE_EP.extensionList.filter { !addedTypes.contains(it.id) }
      val configurationTypes = RunConfigurable.getTypesToShow(project, showApplicableTypesOnly, allTypes).toMutableList()
      configurationTypes.sortWith(IGNORE_CASE_DISPLAY_NAME_COMPARATOR)
      val hiddenCount = allTypes.size - configurationTypes.size
      val popupList = ArrayList<Any>(configurationTypes)
      if (hiddenCount > 0) {
        popupList.add(ExecutionBundle.message("show.irrelevant.configurations.action.name", hiddenCount))
      }

      val builder = JBPopupFactory.getInstance().createPopupChooserBuilder(popupList)
        .setTitle(ExecutionBundle.message("run.dashboard.configurable.add.configuration.type"))
        .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
        .setRenderer(object : ColoredListCellRenderer<Any>() {
          override fun customizeCellRenderer(list: JList<*>,
                                             value: Any,
                                             index: Int,
                                             selected: Boolean,
                                             hasFocus: Boolean) {
            if (value is ConfigurationType) {
              icon = value.icon
              append(value.displayName)
            }
            else {
              append(value.toString())
            }
          }
        })
        .setMovable(true)
        .setResizable(true)
        .setNamerForFiltering { if (it is ConfigurationType) it.displayName else null }
        .setAdText("Select one or more types")
        .setItemsChosenCallback { selectedValues ->
          val value = ContainerUtil.getOnlyItem(selectedValues)
          if (value is String) {
            showAddPopup(project, addedTypes, onAddCallback, popupOpener, false)
            return@setItemsChosenCallback
          }

          onAddCallback.accept(selectedValues.filterIsInstance<ConfigurationType>())
        }
      popupOpener.accept(builder.createPopup())
    }
  }
}
