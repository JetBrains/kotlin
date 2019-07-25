// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.config

import com.intellij.codeInsight.hints.HintUtils
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel

class InlayHintsConfigurable(val project: Project) : Configurable, Configurable.Composite {
  private val configurables: List<SingleLanguageInlayHintsConfigurable> = HintUtils.getLanguagesWithHintsSupport(project)
    .map { SingleLanguageInlayHintsConfigurable(project, it) }
    .sortedBy { it.displayName }

  override fun getConfigurables(): Array<Configurable> {
    return configurables.toTypedArray()
  }

  private val panel = JPanel().also {
    it.layout = BoxLayout(it, BoxLayout.Y_AXIS)
    it.border = JBUI.Borders.empty(0, 10, 0, 0)
    for (configurable in configurables) {
      val label = LinkLabel.create(configurable.language.displayName) {
        val settings = Settings.KEY.getData(DataManager.getInstance().getDataContext(it))
        settings?.select(configurable)
      }
      it.add(label)
    }
  }

  override fun isModified(): Boolean {
    return false
  }

  override fun getDisplayName(): String {
    return "Inlay Hints"
  }

  override fun createComponent(): JComponent {
    return panel
  }

  override fun apply() {

  }

  fun loadFromSettings() {
    for (configurable in configurables) {
      configurable.loadFromSettings()
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