// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.settings.InlaySettingsProvider
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.lang.Language
import com.intellij.openapi.project.Project

class InlayProviderUsageCollector : ProjectUsagesCollector() {
  override fun getGroupId(): String {
    return "inlay.configuration"
  }

  override fun getMetrics(project: Project): MutableSet<MetricEvent> {
    val settingsProviders = InlaySettingsProvider.EP.getExtensions()
    val metricEvents = mutableSetOf<MetricEvent>()
    val settings = InlayHintsSettings.instance()
    metricEvents.add(MetricEvent("global.inlays.settings",
                                 FeatureUsageData().addData("enabled_globally", settings.hintsEnabledGlobally())))
    for (settingsProvider in settingsProviders) {
      val languages = settingsProvider.getSupportedLanguages(project)
      for (language in languages) {
        val models = settingsProvider.createModels(project, language)
        for (model in models) {
          addModelEvents(model, language, metricEvents)
        }
        metricEvents.add(MetricEvent("language.inlays.settings", FeatureUsageData()
          .addData("enabled", settings.hintsEnabled(language))
          .addLanguage(language)
        ))
      }
    }
    return metricEvents
  }

  private fun addModelEvents(model: InlayProviderSettingsModel,
                             language: Language,
                             metrics: MutableSet<MetricEvent>) {
    for (case in model.cases) {
      val usageData = FeatureUsageData()
        .addData("model", model.id)
        .addData("option_id", case.id)
        .addData("option_value", case.value)
        .addLanguage(language)
        .addData("enabled", model.isEnabled)
      metrics.add(MetricEvent("model.options", usageData))
    }
  }

  override fun getVersion(): Int {
    return 3
  }
}