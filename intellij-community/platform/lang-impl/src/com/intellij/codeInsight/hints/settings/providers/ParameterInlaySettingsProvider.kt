// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.providers

import com.intellij.codeInsight.hints.InlayParameterHintsExtension
import com.intellij.codeInsight.hints.PARAMETER_NAME_HINTS_EP
import com.intellij.codeInsight.hints.getBaseLanguagesWithProviders
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.settings.InlaySettingsProvider
import com.intellij.codeInsight.hints.settings.language.ParameterInlayProviderSettingsModel
import com.intellij.lang.Language
import com.intellij.openapi.extensions.BaseExtensionPointName
import com.intellij.openapi.project.Project

class ParameterInlaySettingsProvider : InlaySettingsProvider {
  override fun createModels(project: Project, language: Language): List<InlayProviderSettingsModel> {
    val provider = InlayParameterHintsExtension.forLanguage(language)
    if (provider != null) {
      return listOf(ParameterInlayProviderSettingsModel(provider, language))
    }
    return emptyList()
  }

  override fun getSupportedLanguages(project: Project): Collection<Language> {
    return getBaseLanguagesWithProviders()
  }

  override fun getDependencies(): Collection<BaseExtensionPointName<*>> {
    return listOf(PARAMETER_NAME_HINTS_EP)
  }
}