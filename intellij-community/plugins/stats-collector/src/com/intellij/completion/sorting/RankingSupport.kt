// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.completion.sorting

import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.internal.ml.completion.RankingModelProvider
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Disposer
import com.intellij.stats.experiment.EmulatedExperiment
import com.intellij.stats.experiment.WebServiceStatus
import com.jetbrains.completion.ranker.WeakModelProvider
import org.jetbrains.annotations.TestOnly


object RankingSupport {
  val EP_NAME: ExtensionPointName<RankingModelProvider> = ExtensionPointName("com.intellij.completion.ml.model")
  private val LOG = logger<RankingSupport>()
  private var enabledInTests: Boolean = false

  fun getRankingModel(language: Language): RankingModelWrapper? {
    val provider = findProviderSafe(language)
    return if (provider != null && shouldSortByML(language, provider)) tryGetModel(provider) else null
  }

  fun availableLanguages(): List<String> {
    val registeredLanguages = Language.getRegisteredLanguages()
    return WeakModelProvider.availableProviders()
      .filter { provider ->
        registeredLanguages.any {
          provider.isLanguageSupported(it)
        }
      }.map { it.displayNameInSettings }.distinct().sorted().toList()
  }

  private fun findProviderSafe(language: Language): RankingModelProvider? {
    try {
      return WeakModelProvider.findProvider(language)
    }
    catch (e: IllegalStateException) {
      LOG.error(e)
      return null
    }
  }

  private fun tryGetModel(provider: RankingModelProvider): RankingModelWrapper? {
    try {
      return LanguageRankingModel(provider.model)
    }
    catch (e: Exception) {
      LOG.error("Could not create ranking model '${provider.displayNameInSettings}'", e)
      return null
    }
  }

  private fun shouldSortByML(language: Language, provider: RankingModelProvider): Boolean {
    val application = ApplicationManager.getApplication()
    val webServiceStatus = WebServiceStatus.getInstance()
    if (application.isUnitTestMode) return enabledInTests
    val settings = CompletionMLRankingSettings.getInstance()
    if (application.isEAP && webServiceStatus.isExperimentOnCurrentIDE() && settings.isCompletionLogsSendAllowed) {
      // AB experiment
      return EmulatedExperiment.shouldRank(language, webServiceStatus.experimentVersion())
    }

    return settings.isRankingEnabled && settings.isLanguageEnabled(provider.displayNameInSettings)
  }

  @TestOnly
  fun enableInTests(parentDisposable: Disposable) {
    enabledInTests = true
    Disposer.register(parentDisposable, Disposable { enabledInTests = false })
  }
}
