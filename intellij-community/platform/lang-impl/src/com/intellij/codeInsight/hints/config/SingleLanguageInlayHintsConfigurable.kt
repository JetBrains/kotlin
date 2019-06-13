// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.config

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SingleLanguageInlayHintsConfigurable(project: Project, val language: Language) : Configurable, SearchableConfigurable {
  private val panel: SingleLanguageInlayHintsSettingsPanel by lazy {
    val settings = ServiceManager.getService(InlayHintsSettings::class.java)
    // All configurables operate with copy of settings, that is why we can do live preview
    val providers = HintUtils.getHintProvidersForLanguage(language, project).map { it.withSettingsCopy() }
    val options = providers.map {
      val provider = it.provider
      HintProviderOption(provider.key, provider.name, settings.hintsEnabled(provider.key, language), provider.previewText)
    }
    val keyToProvider = providers.associateBy { it.provider.key }
    val settingsWrappers = providers.map { it.toSettingsWrapper(settings, language) }
    SingleLanguageInlayHintsSettingsPanel(project, language, keyToProvider, settingsWrappers, providers.first(), options)
  }

  override fun getId(): String {
    return "inlay.hints." + language.id
  }


  override fun isModified(): Boolean {
    return panel.isModified()
  }

  override fun getDisplayName(): String {
    return language.displayName
  }

  override fun createComponent(): JComponent? {
    return panel
  }

  override fun apply() {
    panel.apply()
    // Not very fine grained, the other way is to let the InlayHintsProviderFactory update hints
    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
  }

  fun loadFromSettings() {
    panel.loadFromSettings()
  }
}