// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.hints.settings.language.SingleLanguageInlayHintsConfigurable
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.border.EmptyBorder

class InlayHintsConfigurable(val project: Project) : Configurable, Configurable.Composite {
  private val configurables: List<SingleLanguageInlayHintsConfigurable> = InlaySettingsProvider.EP.getExtensions()
    .flatMap { it.getSupportedLanguages(project) }
    .map { SingleLanguageInlayHintsConfigurable(project, it) }
    .sortedBy { it.displayName }

  override fun getConfigurables(): Array<Configurable> {
    return configurables.toTypedArray()
  }

  private val listPanel = createPanel()

  private fun createPanel() : JPanel {
    val outer = JPanel()
    outer.layout = BorderLayout()
    val label = JLabel(CodeInsightBundle.message("inlay.hints.language.list.description"))
    outer.add(label, BorderLayout.NORTH)
    outer.add(createListPanel(), BorderLayout.WEST)
    return outer
  }

  private fun createListPanel(): JPanel {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.border = JBUI.Borders.empty(0, 10, 0, 0)
    panel.add(Box.createRigidArea(JBUI.size(0, 10)))
    for (configurable in configurables) {
      val label = LinkLabel.create(configurable.language.displayName) {
        val settings = Settings.KEY.getData(DataManager.getInstance().getDataContext(panel))
        settings?.select(configurable)
      }
      label.alignmentX = 0f
      label.border = EmptyBorder(1, 17, 3, 1)
      panel.add(label)
    }
    return panel
  }

  override fun isModified(): Boolean {
    return false
  }

  override fun getDisplayName(): String {
    return "Inlay Hints"
  }

  override fun createComponent(): JComponent {
    return listPanel
  }

  override fun apply() {

  }

  fun loadFromSettings() {
    for (configurable in configurables) {
      configurable.reset()
    }
  }

  companion object {
    /**
     * Updates settings UI when external change happens (e. g. when some provider is changed).
     */
    @JvmStatic
    fun updateInlayHintsUI() {
      val dataContextFromFocusAsync = DataManager.getInstance().dataContextFromFocusAsync
      dataContextFromFocusAsync.onSuccess {
        val settings = Settings.KEY.getData(it) ?: return@onSuccess
        val configurable = settings.find(InlayHintsConfigurable::class.java) ?: return@onSuccess
        configurable.loadFromSettings()
      }
    }

    @JvmStatic
    fun showSettingsDialogForLanguage(project: Project, language: Language) {
      val displayName = language.displayName
      ShowSettingsUtil.getInstance()
        .showSettingsDialog(project, { it.displayName == displayName && it is SingleLanguageInlayHintsConfigurable }, {})
    }
  }
}