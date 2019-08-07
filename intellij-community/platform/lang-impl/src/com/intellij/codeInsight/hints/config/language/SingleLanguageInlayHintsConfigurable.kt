// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.config.language

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent


class SingleLanguageInlayHintsConfigurable(project: Project, val language: Language) : Configurable, SearchableConfigurable {
  private val panel by lazy {
    SingleLanguageInlayHintsSettingsPanel(getInlayProviderSettingsModels(project, language), language, project)
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
    ParameterHintsPassFactory.forceHintsUpdateOnNextPass()
    InlayHintsPassFactory.forceHintsUpdateOnNextPass()
  }

  override fun getId(): String {
    return "inlay.hints." + language.id
  }

  companion object {
    fun getInlayProviderSettingsModels(project: Project, language: Language) : Array<InlayProviderSettingsModel> {
      val parameterHintsProvider = InlayParameterHintsExtension.forLanguage(language)

      val models = mutableListOf<InlayProviderSettingsModel>()
      if (parameterHintsProvider != null) {
        models += ParameterInlayProviderSettingsModel(parameterHintsProvider, language)
      }

      val config = InlayHintsSettings.instance()
      HintUtils.getHintProvidersForLanguage(language, project).mapTo(models) {
        NewInlayProviderSettingsModel(it.withSettingsCopy(), config)
      }
      return models.toTypedArray()
    }
  }

  override fun reset() {
    panel.reset()
  }
}



