// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.lang.Language
import com.intellij.openapi.project.Project


object HintUtils {
  private fun getAllMetaProviders() : List<InlayHintsProviderFactory> {
    return InlayHintsProviderFactory.EP.extensionList
  }

  fun getLanguagesWithNewInlayHints(project: Project) : Set<Language> {
    val languages = HashSet<Language>()
    getAllMetaProviders().flatMapTo(languages) { it.getProvidersInfo(project).map { info -> info.language } }
    return languages
  }

  fun getHintProvidersForLanguage(language: Language, project: Project): List<ProviderWithSettings<out Any>> {
    val config = InlayHintsSettings.instance()
    return getAllMetaProviders()
      .flatMap { it.getProvidersInfo(project) }
      .filter { language.isKindOf(it.language) && it.provider.isLanguageSupported(language) }
      .map { it.provider.withSettings(it.language, config) }
  }
}

