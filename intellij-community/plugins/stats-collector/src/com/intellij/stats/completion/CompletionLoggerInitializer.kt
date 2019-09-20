// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.registry.Registry
import com.intellij.reporting.isUnitTestMode
import com.intellij.stats.experiment.WebServiceStatus
import kotlin.random.Random

class CompletionLoggerInitializer(private val actionListener: LookupActionsListener) : LookupTracker() {
  companion object {
    fun shouldInitialize(): Boolean =
      (ApplicationManager.getApplication().isEAP && StatisticsUploadAssistant.isSendAllowed()) || isUnitTestMode()

    private val LOGGED_SESSIONS_RATIO: Map<String, Double> = mapOf(
      "python" to 0.5,
      "scala" to 0.3,
      "php" to 0.2,
      "kotlin" to 0.2,
      "java" to 0.1
    )
  }

  override fun lookupClosed() {
    actionListener.listener = CompletionPopupListener.Adapter()
  }

  override fun lookupCreated(language: Language?, lookup: LookupImpl) {
    if (isUnitTestMode() && !CompletionTrackerInitializer.isEnabledInTests) return

    val experimentHelper = WebServiceStatus.getInstance()
    if (sessionShouldBeLogged(experimentHelper, language)) {
      val tracker = actionsTracker(lookup, experimentHelper)
      actionListener.listener = tracker
      lookup.addLookupListener(tracker)
      lookup.setPrefixChangeListener(tracker)
    }
    else {
      actionListener.listener = CompletionPopupListener.Adapter()
    }
  }

  private fun actionsTracker(lookup: LookupImpl, experimentHelper: WebServiceStatus): CompletionActionsTracker {
    val logger = CompletionLoggerProvider.getInstance().newCompletionLogger()
    return CompletionActionsTracker(lookup, logger, experimentHelper)
  }

  private fun sessionShouldBeLogged(experimentHelper: WebServiceStatus, language: Language?): Boolean {
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode || experimentHelper.isExperimentOnCurrentIDE()) return true

    if (!CompletionMLRankingSettings.getInstance().isCompletionLogsSendAllowed ||
        Registry.`is`("completion.stats.show.ml.ranking.diff"))
      return false

    var logSessionChance = 0.0
    if (language != null) {
      logSessionChance = LOGGED_SESSIONS_RATIO.getOrDefault(language.displayName.toLowerCase(), 1.0)
    }

    return Random.nextDouble() < logSessionChance
  }
}
