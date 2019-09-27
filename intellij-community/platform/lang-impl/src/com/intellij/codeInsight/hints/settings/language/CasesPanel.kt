// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.language

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.util.ui.JBUI
import java.awt.Component
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JPanel

internal class CasesPanel(
  cases: List<ImmediateConfigurable.Case>,
  mainCheckBoxName: String,
  private val loadMainCheckBoxValue: () -> Boolean,
  private val onUserChangedMainCheckBox: (Boolean) -> Unit,
  listener: ChangeListener
) : JPanel() {
  private val caseListPanel = CaseListPanel(cases, listener)
  private val mainCheckBox = JCheckBox(mainCheckBoxName, loadMainCheckBoxValue())

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    add(mainCheckBox)
    if (cases.isNotEmpty()) {
      add(caseListPanel)
    }
    mainCheckBox.addActionListener {
      val selected = mainCheckBox.isSelected
      caseListPanel.setEnabledCheckboxes(selected)
      onUserChangedMainCheckBox(selected)
    }
    caseListPanel.setEnabledCheckboxes(loadMainCheckBoxValue())
    revalidate()
  }

  fun updateFromSettings() {
    val mainCheckBoxSelected = loadMainCheckBoxValue()
    mainCheckBox.isSelected = mainCheckBoxSelected
    caseListPanel.setEnabledCheckboxes(mainCheckBoxSelected)
    caseListPanel.updateCheckBoxes()
  }
}

private class CaseListPanel(val cases: List<ImmediateConfigurable.Case>, listener: ChangeListener) : JPanel() {
  val checkBoxes = mutableListOf<JCheckBox>()

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = JBUI.Borders.empty(0, 20, 0, 0)
    add(Box.createRigidArea(JBUI.size(0, 5)))
    for (case in cases) {
      val checkBox = JCheckBox(case.name, case.value)
      add(checkBox)
      checkBox.alignmentX = Component.LEFT_ALIGNMENT
      checkBoxes.add(checkBox)
      checkBox.addActionListener {
        case.value = checkBox.isSelected
        listener.settingsChanged()
      }
      add(Box.createRigidArea(Dimension(0, 3)))
    }
    add(Box.createRigidArea(JBUI.size(0, 5)))
  }

  fun setEnabledCheckboxes(value: Boolean) {
    for (checkBox in checkBoxes) {
      checkBox.isEnabled = value
    }
  }

  fun updateCheckBoxes() {
    for ((index, checkBox) in checkBoxes.withIndex()) {
      checkBox.isSelected = cases[index].value
    }
  }
}