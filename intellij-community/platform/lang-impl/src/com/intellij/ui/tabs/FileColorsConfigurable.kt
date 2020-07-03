// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs

import com.intellij.icons.AllIcons
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.Configurable.NoScroll
import com.intellij.ide.IdeBundle.message
import com.intellij.ide.util.scopeChooser.EditScopesDialog
import com.intellij.openapi.actionSystem.ActionToolbarPosition
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.CheckBoxConfigurable
import com.intellij.openapi.options.UnnamedConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.packageDependencies.DependencyValidationManager
import com.intellij.psi.search.scope.packageSet.NamedScope
import com.intellij.psi.search.scope.packageSet.NamedScopeManager
import com.intellij.ui.ColorChooser.chooseColor
import com.intellij.ui.ColorUtil.toHex
import com.intellij.ui.FileColorManager
import com.intellij.ui.ToolbarDecorator.createDecorator
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.EditableModel
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.PaintIcon
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

class FileColorsConfigurable(project: Project) : SearchableConfigurable, NoScroll {
  override fun getId() = "reference.settings.ide.settings.file-colors"
  override fun getDisplayName(): String = message("configurable.file.colors")
  override fun getHelpTopic() = id

  private val enabledFileColors = object : CheckBoxConfigurable() {
    override fun createCheckBox(): JCheckBox {
      val checkBox = JCheckBox(message("settings.file.colors.enable.file.colors"))
      checkBox.addChangeListener {
        useInEditorTabs.checkBox?.isEnabled = checkBox.isSelected
        useInProjectView.checkBox?.isEnabled = checkBox.isSelected
      }
      return checkBox
    }

    override var selectedState: Boolean
      get() = manager.isEnabled
      set(state) {
        manager.isEnabled = state
      }
  }

  private val useInEditorTabs = object : CheckBoxConfigurable() {
    override fun createCheckBox(): JCheckBox {
      val checkBox = JCheckBox(message("settings.file.colors.use.in.editor.tabs"))
      checkBox.isEnabled = false
      return checkBox
    }

    override var selectedState: Boolean
      get() = manager.isEnabledForTabs
      set(state) {
        manager.isEnabledForTabs = state
      }
  }

  private val useInProjectView = object : CheckBoxConfigurable() {
    override fun createCheckBox(): JCheckBox {
      val checkBox = JCheckBox(message("settings.file.colors.use.in.project.vew"))
      checkBox.isEnabled = false
      return checkBox
    }

    override var selectedState: Boolean
      get() = manager.isEnabledForProjectView
      set(state) {
        FileColorManagerImpl.setEnabledForProjectView(state)
      }
  }

  private val manager = FileColorManager.getInstance(project) as FileColorManagerImpl
  private val colorsTableModel = FileColorsTableModel(manager)
  private val configurables = listOf(enabledFileColors, useInEditorTabs, useInProjectView, colorsTableModel)

  // UnnamedConfigurable

  override fun createComponent(): JPanel? {
    disposeUIResources()

    val north = JPanel(HorizontalLayout(10))
    north.border = Borders.emptyBottom(5)
    north.add(HorizontalLayout.LEFT, enabledFileColors.createComponent())
    north.add(HorizontalLayout.LEFT, useInEditorTabs.createComponent())
    north.add(HorizontalLayout.LEFT, useInProjectView.createComponent())

    val south = JPanel(VerticalLayout(5))
    south.border = Borders.emptyTop(5)
    south.add(VerticalLayout.TOP, JLabel(message("settings.file.colors.description")))
    south.add(VerticalLayout.TOP, LinkLabel.create(message("settings.file.colors.manage.scopes")) {
      EditScopesDialog.showDialog(manager.project, null, true)
    })

    val panel = JPanel(BorderLayout())
    panel.add(BorderLayout.NORTH, north)
    panel.add(BorderLayout.CENTER, colorsTableModel.createComponent())
    panel.add(BorderLayout.SOUTH, south)
    return panel
  }

  override fun isModified() = configurables.any { it.isModified }
  override fun apply() = configurables.forEach { it.apply() }
  override fun reset() = configurables.forEach { it.reset() }
  override fun disposeUIResources() = configurables.forEach { it.disposeUIResources() }
}

// table support

private class Column(private val key: String, val type: Class<*>, val editable: Boolean) {
  val name: String
    get() = message(key)
}

private val columns = arrayOf(
  Column("settings.file.color.column.scope", String::class.java, false),
  Column("settings.file.color.column.color", FileColorConfiguration::class.java, true),
  Column("settings.file.color.column.shared", Boolean::class.javaObjectType, true))

private class FileColorsTableModel(val manager: FileColorManagerImpl) : AbstractTableModel(), EditableModel, UnnamedConfigurable {
  private val local = mutableListOf<FileColorConfiguration>()
  private val shared = mutableListOf<FileColorConfiguration>()
  private var table: JTable? = null

  private fun copy(list: List<FileColorConfiguration>) = list.map { copy(it) }
  private fun copy(configuration: FileColorConfiguration) = FileColorConfiguration(configuration.scopeName, configuration.colorName)

  private fun selectRow(row: Int) {
    val table = table ?: return
    table.setRowSelectionInterval(row, row)
    table.scrollRectToVisible(table.getCellRect(row, 0, true))
  }

  private fun getConfiguration(row: Int): FileColorConfiguration? {
    val index = getSharedIndex(row) ?: return null
    return if (index < 0) local[row] else shared[index]
  }

  private fun getSharedIndex(row: Int): Int? {
    if (row < 0) return null
    val index = row - local.size
    return if (index < shared.size) index else null
  }

  private fun resolveCustomColor(value: Any?): String? {
    val name = value as? String ?: return null
    if (null != manager.getColor(name)) return name
    val parent = table ?: return null
    return chooseColor(parent, message("settings.file.colors.dialog.choose.color"), null)?.let { toHex(it) }
  }

  private fun resolveDuplicate(scopeName: String, colorName: String, toSharedList: Boolean): Boolean {
    val list = if (toSharedList) shared else local
    val index = list.indexOfFirst { it.scopeName == scopeName }
    if (index < 0) return false
    val parent = table ?: return false
    val title = when (toSharedList) {
      true -> message("settings.file.colors.dialog.warning.shared", scopeName)
      else -> message("settings.file.colors.dialog.warning.local", scopeName)
    }
    val configuration = list[index]
    val update = when (configuration.colorName == colorName) {
      true -> {
        Messages.YES != Messages.showYesNoDialog(
          parent,
          message("settings.file.colors.dialog.warning.append"),
          title,
          Messages.getWarningIcon())
      }
      else -> {
        val oldColor = manager.getColor(configuration.colorName)?.let { toHex(it) } ?: ""
        val newColor = manager.getColor(colorName)?.let { toHex(it) } ?: ""
        Messages.OK == Messages.showOkCancelDialog(
          parent,
          message("settings.file.colors.dialog.warning.replace",
                  oldColor,
                  newColor),
          title,
          message("settings.file.colors.dialog.warning.update"),
          Messages.getCancelButton(),
          Messages.getWarningIcon())
      }
    }
    if (!update) return false
    configuration.colorName = colorName
    val row = if (toSharedList) local.size + index else index
    fireTableRowsUpdated(row, row)
    selectRow(row)
    return true
  }

  private fun onRowInserted(row: Int) {
    fireTableRowsInserted(row, row)
    selectRow(row)
  }

  internal fun addScopeColor(scope: NamedScope, color: String?) {
    val colorName = resolveCustomColor(color) ?: return
    if (resolveDuplicate(scope.name, colorName, false)) return
    local.add(0, FileColorConfiguration(scope.name, colorName))
    onRowInserted(0)
  }

  internal fun getScopes(): List<NamedScope> {
    val list = mutableListOf<NamedScope>()
    list += DependencyValidationManager.getInstance(manager.project).scopes
    list += NamedScopeManager.getInstance(manager.project).scopes
    return list.filter { it.value != null }
  }

  internal fun getColors(): List<String> {
    val list = mutableListOf<String>()
    list += manager.colorNames
    list += message("settings.file.color.custom.name")
    return list
  }

  // TableModel

  override fun getColumnCount() = columns.size

  override fun getColumnName(column: Int) = columns[column].name

  override fun getColumnClass(column: Int) = columns[column].type

  override fun isCellEditable(row: Int, column: Int) = columns[column].editable

  override fun getRowCount() = local.size + shared.size

  override fun getValueAt(row: Int, column: Int): Any? {
    return when (column) {
      0 -> getConfiguration(row)?.scopeName
      1 -> getConfiguration(row)
      2 -> row >= local.size
      else -> null
    }
  }

  override fun setValueAt(value: Any?, row: Int, column: Int) {
    when (column) {
      1 -> {
        val configuration = getConfiguration(row) ?: return
        configuration.colorName = resolveCustomColor(value) ?: return
        fireTableCellUpdated(row, column)
      }
      2 -> {
        val index = getSharedIndex(row) ?: return
        if (index < 0) {
          val configuration = local.removeAt(row)
          fireTableRowsDeleted(row, row)
          if (resolveDuplicate(configuration.scopeName, configuration.colorName, true)) return
          shared.add(0, configuration)
          onRowInserted(local.size)
        }
        else if (index < shared.size) {
          val configuration = shared.removeAt(index)
          fireTableRowsDeleted(row, row)
          if (resolveDuplicate(configuration.scopeName, configuration.colorName, false)) return
          local.add(configuration)
          onRowInserted(local.size - 1)
        }
      }
    }
  }

  // EditableModel

  override fun addRow() = throw UnsupportedOperationException()

  override fun removeRow(row: Int) {
    val index = getSharedIndex(row) ?: return
    if (index < 0) local.removeAt(row) else shared.removeAt(index)
    fireTableRowsDeleted(row, row)
  }

  override fun exchangeRows(oldRow: Int, newRow: Int) {
    if (oldRow == newRow) return
    val oldIndex = getSharedIndex(oldRow) ?: return
    val newIndex = getSharedIndex(newRow) ?: return
    when {
      (oldIndex < 0) && (newIndex < 0) -> exchangeRows(local, oldRow, newRow)
      oldIndex < 0 || newIndex < 0 -> return // cannot move from local to shared and vice versa
      else -> exchangeRows(shared, oldIndex, newIndex)
    }
    fireTableRowsUpdated(oldRow, oldRow)
    fireTableRowsUpdated(newRow, newRow)
  }

  private fun exchangeRows(list: MutableList<FileColorConfiguration>, oldIndex: Int, newIndex: Int) {
    val maxIndex = oldIndex.coerceAtLeast(newIndex)
    val minIndex = oldIndex.coerceAtMost(newIndex)
    val maxConfiguration = list.removeAt(maxIndex)
    val minConfiguration = list.removeAt(minIndex)
    list.add(minIndex, maxConfiguration)
    list.add(maxIndex, minConfiguration)
  }

  override fun canExchangeRows(oldRow: Int, newRow: Int): Boolean {
    if (oldRow == newRow) return true
    val oldIndex = getSharedIndex(oldRow) ?: return false
    val newIndex = getSharedIndex(newRow) ?: return false
    return (oldIndex < 0) == (newIndex < 0)
  }

  // UnnamedConfigurable

  override fun createComponent(): JComponent {
    val table = JBTable(this)
    table.emptyText.text = message("settings.file.colors.no.colors.specified")

    this.table = table

    table.setDefaultRenderer(String::class.java, TableScopeRenderer(manager))
    // configure color renderer and its editor
    val editor = ComboBox<String>(getColors().toTypedArray())
    editor.renderer = ComboBoxColorRenderer(manager)
    table.setDefaultEditor(FileColorConfiguration::class.java, DefaultCellEditor(editor))
    table.setDefaultRenderer(FileColorConfiguration::class.java, TableColorRenderer(manager))
    // align boolean renderer to left
    val booleanRenderer = table.getDefaultRenderer(Boolean::class.javaObjectType)
    val rendererCheckBox = booleanRenderer as? JCheckBox
    rendererCheckBox?.horizontalAlignment = SwingConstants.LEFT
    // align boolean editor to left
    val booleanEditor = table.getDefaultEditor(Boolean::class.javaObjectType)
    val editorWrapper = booleanEditor as? DefaultCellEditor
    val editorCheckBox = editorWrapper?.component as? JCheckBox
    editorCheckBox?.horizontalAlignment = SwingConstants.LEFT
    // create and configure table decorator
    return createDecorator(table)
      .setAddAction {
        val popup = JBPopupFactory.getInstance().createListPopup(ScopeListPopupStep(this))
        it.preferredPopupPoint?.let { point -> popup.show(point) }
      }
      .setMoveUpActionUpdater { table.selectedRows.all { canExchangeRows(it, it - 1) } }
      .setMoveDownActionUpdater { table.selectedRows.all { canExchangeRows(it, it + 1) } }
      .setToolbarPosition(ActionToolbarPosition.TOP)
      .createPanel()
  }

  override fun isModified(): Boolean {
    return local != manager.applicationLevelConfigurations || shared != manager.projectLevelConfigurations
  }

  override fun apply() {
    manager.model.setConfigurations(copy(local), false)
    manager.model.setConfigurations(copy(shared), true)
  }

  override fun reset() {
    local.clear()
    local.addAll(copy(manager.applicationLevelConfigurations))
    shared.clear()
    shared.addAll(copy(manager.projectLevelConfigurations))
    fireTableDataChanged()
  }
}

// renderers

private fun updateColorRenderer(renderer: JLabel, selected: Boolean, background: Color?): JLabel {
  if (!selected) renderer.background = background
  renderer.horizontalTextPosition = SwingConstants.LEFT
  renderer.icon = background?.let { if (selected) PaintIcon(36, 12, it).withIconPreScaled(false) else null }
  return renderer
}

private class ComboBoxColorRenderer(val manager: FileColorManagerImpl) : DefaultListCellRenderer() {
  override fun getListCellRendererComponent(list: JList<*>?, value: Any?, index: Int, selected: Boolean, focused: Boolean): Component {
    super.getListCellRendererComponent(list, value, index, selected, focused)
    return updateColorRenderer(this, selected, value?.toString()?.let { manager.getColor(it) })
  }
}

private class TableColorRenderer(val manager: FileColorManagerImpl) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(table: JTable?, value: Any?,
                                             selected: Boolean, focused: Boolean, row: Int, column: Int): Component {
    val configuration = value as? FileColorConfiguration
    super.getTableCellRendererComponent(table, configuration?.colorPresentableName, selected, focused, row, column)
    return updateColorRenderer(this, selected, configuration?.colorName?.let { manager.getColor(it) })
  }
}

private class TableScopeRenderer(val manager: FileColorManagerImpl) : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(table: JTable?, value: Any?,
                                             selected: Boolean, focused: Boolean, row: Int, column: Int): Component {
    val component = super.getTableCellRendererComponent(table, value, selected, focused, row, column)
    val unknown = null == value?.toString()?.let { manager.model.getScopeColor(it, manager.project) }
    toolTipText = if (unknown) message("settings.file.colors.scope.unknown") else null
    icon = if (unknown) AllIcons.General.Error else null
    return component
  }
}

// popup steps

private class ScopeListPopupStep(val model: FileColorsTableModel)
  : BaseListPopupStep<NamedScope>(null, model.getScopes()) {
  override fun getTextFor(scope: NamedScope?) = scope?.name ?: ""
  override fun getIconFor(scope: NamedScope?) = scope?.icon
  override fun hasSubstep(selectedValue: NamedScope?) = true
  override fun onChosen(scope: NamedScope?, finalChoice: Boolean): PopupStep<*>? {
    return scope?.let { ColorListPopupStep(model, it) }
  }
}

private class ColorListPopupStep(val model: FileColorsTableModel, val scope: NamedScope)
  : BaseListPopupStep<String>(null, model.getColors()) {
  override fun getBackgroundFor(value: String?) = value?.let { model.manager.getColor(it) }
  override fun onChosen(value: String?, finalChoice: Boolean): PopupStep<*>? {
    // invoke later to close popup before showing dialog
    invokeLater { model.addScopeColor(scope, value) }
    return null
  }
}
