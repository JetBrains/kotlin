// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.stats.CompletionStatsPolicy
import com.intellij.stats.experiment.WebServiceStatus
import com.intellij.stats.storage.factors.MutableLookupStorage
import kotlin.random.Random

class CompletionLoggerInitializer(private val actionListener: LookupActionsListener) : LookupTracker() {
  companion object {
    fun shouldInitialize(): Boolean =
      (ApplicationManager.getApplication().isEAP && StatisticsUploadAssistant.isSendAllowed()) || ApplicationManager.getApplication().isUnitTestMode

    private val LOGGED_SESSIONS_RATIO: Map<String, Double> = mapOf(
      "python" to 0.5,
      "scala" to 0.3,
      "php" to 0.2,
      "kotlin" to 0.2,
      "java" to 0.1,
      "ecmascript 6" to 0.2,
      "typescript" to 0.5,
      "c/c++" to 0.5,
      "c#" to 0.1,
      "go" to 0.4
    )
  }

  override fun lookupClosed() {
    actionListener.listener = CompletionPopupListener.Adapter()
  }

  override fun lookupCreated(lookup: LookupImpl,
                             storage: MutableLookupStorage) {
    if (ApplicationManager.getApplication().isUnitTestMode && !CompletionTrackerInitializer.isEnabledInTests) return

    val experimentHelper = WebServiceStatus.getInstance()
    if (sessionShouldBeLogged(experimentHelper, storage.language)) {
      val tracker = actionsTracker(lookup, storage, experimentHelper)
      actionListener.listener = tracker
      lookup.addLookupListener(tracker)
      lookup.setPrefixChangeListener(tracker)
      storage.markLoggingEnabled()
    }
    else {
      actionListener.listener = CompletionPopupListener.Adapter()
    }
  }

  private fun actionsTracker(lookup: LookupImpl,
                             storage: MutableLookupStorage,
                             experimentHelper: WebServiceStatus): CompletionActionsListener {
    val logger = CompletionLoggerProvider.getInstance().newCompletionLogger()
    val actionsTracker = CompletionActionsTracker(lookup, storage, logger, experimentHelper)
    return LoggerPerformanceTracker(actionsTracker, storage.performanceTracker)
  }

  private fun sessionShouldBeLogged(experimentHelper: WebServiceStatus, language: Language): Boolean {
    if (CompletionStatsPolicy.isStatsLogDisabled(language) || !getPluginInfo(language::class.java).isSafeToReport()) return false
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode || experimentHelper.isExperimentOnCurrentIDE()) return true

    if (!CompletionMLRankingSettings.getInstance().isCompletionLogsSendAllowed)
      return false

    val logSessionChance = LOGGED_SESSIONS_RATIO.getOrDefault(language.displayName.toLowerCase(), 1.0)

    return Random.nextDouble() < logSessionChance
  }
}
