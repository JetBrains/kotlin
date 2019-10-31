// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.settings.language.SingleLanguageInlayHintsConfigurable
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class InlayHintsConfigurable(val project: Project) : Configurable, Configurable.Composite {
  private val settings = InlayHintsSettings.instance()
  private val configurables: List<SingleLanguageInlayHintsConfigurable>
  private val panel: InlayHintsPanel

  init {
    val allInlayLanguages = InlaySettingsProvider.EP.getExtensions()
      .flatMap { it.getSupportedLanguages(project) }
      .toSortedSet(compareBy { it.displayName })
    configurables = allInlayLanguages.map { SingleLanguageInlayHintsConfigurable(project, it) }
    panel = InlayHintsPanel(allInlayLanguages, settings)

    ApplicationManager.getApplication().messageBus.connect(project).subscribe(
      InlayHintsSettings.INLAY_SETTINGS_CHANGED,
      ConfigurationChangeListener(configurables))
  }

  override fun getConfigurables(): Array<Configurable> {
    return configurables.toTypedArray()
  }


  override fun isModified(): Boolean {
    return panel.isModified()
  }

  override fun getDisplayName(): String {
    return "Inlay Hints"
  }

  override fun createComponent(): JComponent {
    return panel
  }

  override fun apply() {
    panel.apply()
  }

  override fun reset() {
    panel.reset()
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

  private class ConfigurationChangeListener(val configurables: List<Configurable>) : InlayHintsSettings.SettingsListener {
    override fun didLanguageStatusChanged() {
      reset()
    }

    override fun didGlobalEnabledStatusChanged(newEnabled: Boolean) {
      reset()
    }

    private fun reset() {
      for (configurable in configurables) {
        configurable.reset()
      }
    }
  }
}

