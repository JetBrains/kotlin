// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

private val inlayProviderName = ExtensionPointName<LanguageExtensionPoint<InlayHintsProvider<*>>>("com.intellij.codeInsight.inlayProvider")

class SingleLanguageInlayProviderFactory : InlayHintsProviderFactory {
  override fun getProvidersInfo(project: Project): List<ProviderWithSettings<*>> {
    val languages = findLanguagesWithHintsSupport()
    val config = ServiceManager.getService(InlayHintsSettings::class.java)
    return languages.mapNotNull { Language.findLanguageByID(it) }
      .flatMap { language ->
        InlayHintsProviderExtension.allForLanguage(language).map { it.withSettings(language, config) }
      }
  }

  private fun findLanguagesWithHintsSupport(): Set<String> {
    val extensionPointName = inlayProviderName
    return extensionPointName.extensionList.map { it.language }.toSet()
  }
}