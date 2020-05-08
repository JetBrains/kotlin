// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.hints.InlayHintsProviderExtension
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.settings.language.SingleLanguageInlayHintsConfigurable
import com.intellij.ide.DataManager
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.options.ex.Settings
import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBusConnection
import java.util.function.Predicate
import javax.swing.JComponent

class InlayHintsConfigurable(val project: Project) : Configurable, Configurable.Composite, Configurable.WithEpDependencies {
  private val settings = InlayHintsSettings.instance()
  private val configurables: List<SingleLanguageInlayHintsConfigurable>
  private val panel: InlayHintsPanel
  private var connection: MessageBusConnection? = null

  init {
    val allInlayLanguages = InlaySettingsProvider.EP.getExtensions()
      .flatMap { it.getSupportedLanguages(project) }
      .toSortedSet(compareBy { it.displayName })
    configurables = allInlayLanguages.map { SingleLanguageInlayHintsConfigurable(project, it) }
    panel = InlayHintsPanel(allInlayLanguages, settings)
  }

  override fun getConfigurables(): Array<Configurable> {
    return configurables.toTypedArray()
  }

  override fun isModified(): Boolean {
    return panel.isModified()
  }

  override fun getDisplayName(): String {
    return CodeInsightBundle.message("settings.inlay.hints.panel.name")
  }

  override fun createComponent(): JComponent {
    connection = ApplicationManager.getApplication().messageBus.connect(project)
    connection?.subscribe(InlayHintsSettings.INLAY_SETTINGS_CHANGED, ConfigurationChangeListener(configurables))

    return panel
  }

  override fun apply() {
    panel.apply()
  }

  override fun reset() {
    panel.reset()
  }

  override fun disposeUIResources() {
    connection?.disconnect()
  }

  fun loadFromSettings() {
    for (configurable in configurables) {
      configurable.reset()
    }
  }

  override fun getDependencies(): Collection<BaseExtensionPointName<*>> =
    listOf(InlaySettingsProvider.EP.EXTENSION_POINT_NAME, InlayHintsProviderExtension.inlayProviderName) +
    InlaySettingsProvider.EP.getExtensions().flatMap { it.getDependencies() }

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
      showSettingsDialogForLanguage(project, language, null)
    }

    @JvmStatic
    fun showSettingsDialogForLanguage(project: Project, language: Language, selector: Predicate<InlayProviderSettingsModel>?) {
      val languages = hashSetOf<Language>()
      var current: Language? = language
      while (current != null) {
        languages.add(current)
        current = current.baseLanguage
      }
      ShowSettingsUtil.getInstance().showSettingsDialog(
        project,
        { it is SingleLanguageInlayHintsConfigurable && it.language in languages },
        { configurable ->
          if (selector == null) return@showSettingsDialog
          configurable as SingleLanguageInlayHintsConfigurable
          val models = configurable.getModels()
          val model = models.find { selector.test(it) }
          if (model != null) {
            configurable.setCurrentModel(model)
          }
        }
      )
    }
  }

  private class ConfigurationChangeListener(val configurables: List<Configurable>) : InlayHintsSettings.SettingsListener {
    override fun languageStatusChanged() {
      reset()
    }

    override fun globalEnabledStatusChanged(newEnabled: Boolean) {
      reset()
    }

    private fun reset() {
      for (configurable in configurables) {
        configurable.reset()
      }
    }
  }
}

