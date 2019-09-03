// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.internal.ml.completion.RankingModelProvider
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.stats.experiment.EmulatedExperiment
import com.intellij.stats.experiment.WebServiceStatus


object RankingSupport {
  private val EP_NAME: ExtensionPointName<RankingModelProvider> = ExtensionPointName("com.intellij.completion.ml.model")
  private val LOG = logger<RankingSupport>()

  fun getRankingModel(language: Language): RankingModelWrapper {
    val provider = findProviderForLanguage(language)
    return if (provider != null && shouldSortByML(provider)) tryGetModel(provider) else RankingModelWrapper.DISABLED
  }

  fun availableLanguages(): List<String> {
    return EP_NAME.extensionList.map { it.displayNameInSettings }.sorted()
  }

  private fun findProviderForLanguage(language: Language): RankingModelProvider? {
    val suitableProviders = EP_NAME.extensionList.filter { it.isLanguageSupported(language) }
    if (suitableProviders.size > 1) {
      LOG.error("Too many sorters found for '${language.displayName}' language: ${suitableProviders.map { it.javaClass.canonicalName }}")
      return null
    }

    return suitableProviders.singleOrNull()
  }

  private fun tryGetModel(provider: RankingModelProvider): RankingModelWrapper {
    try {
      return LanguageRankingModel(provider.model)
    }
    catch (e: Exception) {
      LOG.error("Could not create ranking model '${provider.displayNameInSettings}'", e)
      return RankingModelWrapper.DISABLED
    }
  }

  private fun shouldSortByML(provider: RankingModelProvider): Boolean {
    val application = ApplicationManager.getApplication()
    val webServiceStatus = WebServiceStatus.getInstance()
    if (application.isUnitTestMode) return false
    val settings = CompletionMLRankingSettings.getInstance()
    if (application.isEAP && webServiceStatus.isExperimentOnCurrentIDE() && settings.isCompletionLogsSendAllowed) {
      // AB experiment
      return EmulatedExperiment.shouldRank(webServiceStatus.experimentVersion())
    }

    return settings.isRankingEnabled && settings.isLanguageEnabled(provider.displayNameInSettings)
  }
}
