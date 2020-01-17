// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings.providers

import com.intellij.codeInsight.hints.HintUtils
import com.intellij.codeInsight.hints.InlayHintsSettings
import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.settings.InlaySettingsProvider
import com.intellij.codeInsight.hints.settings.language.NewInlayProviderSettingsModel
import com.intellij.codeInsight.hints.withSettingsCopy
import com.intellij.lang.Language
import com.intellij.openapi.project.Project

class NewInlaySettingsProvider : InlaySettingsProvider {
  override fun createModels(project: Project, language: Language): List<InlayProviderSettingsModel> {
    val config = InlayHintsSettings.instance()
    return HintUtils.getHintProvidersForLanguage(language, project).map {
      NewInlayProviderSettingsModel(it.withSettingsCopy(), config)
    }
  }

  override fun getSupportedLanguages(project: Project): Collection<Language> {
    return HintUtils.getLanguagesWithNewInlayHints(project)
  }
}