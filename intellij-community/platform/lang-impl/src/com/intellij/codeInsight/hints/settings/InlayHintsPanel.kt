// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.lang.Language
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class InlayHintsPanel(languages: Iterable<Language>,
                      val settings: InlayHintsSettings) : JPanel() {
  private val hintsEnabledGlobally = JCheckBox("Show hints for:", true)
  private val languagePanels = languages.map { LanguagePanel(it) }

  init {
    layout = BorderLayout()
    val label = JLabel(CodeInsightBundle.message("inlay.hints.language.list.description"))
    add(label, BorderLayout.NORTH)
    add(createListPanel(), BorderLayout.WEST)
    reset()
  }

  private fun createListPanel(): JPanel {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.border = JBUI.Borders.empty(0, 10, 0, 0)
    panel.add(Box.createRigidArea(JBUI.size(0, 10)))
    val toggleGloballyCheckBox = hintsEnabledGlobally
    toggleGloballyCheckBox.addActionListener {
      val selected = toggleGloballyCheckBox.isSelected
      for (languagePanel in languagePanels) {
        languagePanel.setCheckBoxEnabled(selected)
      }
    }
    panel.add(toggleGloballyCheckBox)
    for (languagePanel in languagePanels) {
      languagePanel.alignmentX = 0f
      languagePanel.border = EmptyBorder(1, 17, 3, 1)
      panel.add(languagePanel)
    }
    return panel
  }

  fun isModified() : Boolean {
    if (hintsEnabledGlobally.isSelected != settings.hintsEnabledGlobally()) return true
    for ((index, panel) in languagePanels.withIndex()) {
      val checkboxSelected = languagePanels[index].selected()
      val inSettingsEnabled = settings.hintsEnabled(panel.language)
      if (checkboxSelected != inSettingsEnabled) return true
    }
    return false
  }

  fun apply() {
    settings.setEnabledGlobally(hintsEnabledGlobally.isSelected)
    for ((index, panel) in languagePanels.withIndex()) {
      settings.setHintsEnabledForLanguage(panel.language, languagePanels[index].selected())
    }
  }

  fun reset() {
    hintsEnabledGlobally.isSelected = settings.hintsEnabledGlobally()
    for ((index, panel) in languagePanels.withIndex()) {
      val languagePanel = languagePanels[index]
      languagePanel.select(settings.hintsEnabled(panel.language))
      languagePanel.setCheckBoxEnabled(settings.hintsEnabledGlobally())
    }
  }
}

private class LanguagePanel(val language: Language) : JPanel() {
  val checkBox = JCheckBox(language.displayName)

  init {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    add(checkBox)
  }

  fun selected() :Boolean {
    return checkBox.isSelected
  }

  fun select(value: Boolean) {
    checkBox.isSelected = value
  }

  fun setCheckBoxEnabled(value: Boolean) {
    checkBox.isEnabled = value
  }
}