// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.completion.settings.CompletionMLRankingSettings
import com.intellij.internal.statistic.utils.StatisticsUploadAssistant
import com.intellij.ide.ApplicationInitializedListener
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.util.registry.Registry
import com.intellij.reporting.isUnitTestMode
import com.intellij.stats.experiment.WebServiceStatus
import kotlin.random.Random

class CompletionTrackerInitializer : ApplicationInitializedListener {
  companion object {
    var isEnabledInTests = false
    private val LOGGED_SESSIONS_RATIO: Map<String, Double> = mapOf(
      "python" to 0.5,
      "scala" to 0.3,
      "php" to 0.2,
      "kotlin" to 0.2,
      "java" to 0.1
    )
  }

  private val actionListener = LookupActionsListener()
  private val factorsInitializer = CompletionFactorsInitializer()
  private val lookupTrackerInitializer = object : LookupTracker() {
    override fun lookupClosed() {
      actionListener.listener = CompletionPopupListener.Adapter()
    }

    override fun lookupCreated(language: Language?, lookup: LookupImpl) {
      if (isUnitTestMode() && !isEnabledInTests) return

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
  }

  private fun actionsTracker(lookup: LookupImpl, experimentHelper: WebServiceStatus): CompletionActionsTracker {
    val logger = CompletionLoggerProvider.getInstance().newCompletionLogger()
    return CompletionActionsTracker(lookup, logger, experimentHelper)
  }

  private fun shouldInitialize() = (ApplicationManager.getApplication().isEAP && StatisticsUploadAssistant.isSendAllowed() && !CompletionTrackerDisabler.isDisabled())
                                   || isUnitTestMode()

  private fun shouldTrackSession() = CompletionMLRankingSettings.getInstance().isCompletionLogsSendAllowed || isUnitTestMode()

  private fun sessionShouldBeLogged(experimentHelper: WebServiceStatus, language: Language?): Boolean {
    if (Registry.`is`("completion.stats.show.ml.ranking.diff") || !shouldTrackSession()) return false
    val application = ApplicationManager.getApplication()
    if (application.isUnitTestMode || experimentHelper.isExperimentOnCurrentIDE()) return true

    var logSessionChance = 0.0
    if (language != null) {
      logSessionChance = LOGGED_SESSIONS_RATIO.getOrDefault(language.displayName.toLowerCase(), 1.0)
    }

    return Random.nextDouble() < logSessionChance
  }

  override fun componentsInitialized() {
    if (!shouldInitialize()) {
      return
    }

    val busConnection = ApplicationManager.getApplication().messageBus.connect()
    busConnection.subscribe(AnActionListener.TOPIC, actionListener)
    busConnection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
      override fun projectOpened(project: Project) {
        LookupManager.getInstance(project).addPropertyChangeListener(lookupTrackerInitializer, project)
        LookupManager.getInstance(project).addPropertyChangeListener(factorsInitializer, project)
      }
    })
  }
}