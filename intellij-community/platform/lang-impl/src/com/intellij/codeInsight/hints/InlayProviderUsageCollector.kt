// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.settings.InlayProviderSettingsModel
import com.intellij.codeInsight.hints.settings.InlaySettingsProvider
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.eventLog.StatisticsEventEscaper
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfo
import com.intellij.lang.Language
import com.intellij.openapi.project.Project

class InlayProviderUsageCollector : ProjectUsagesCollector() {
  override fun getGroupId(): String {
    return "inlay.configuration"
  }

  override fun getMetrics(project: Project): MutableSet<MetricEvent> {
    val settingsProviders = InlaySettingsProvider.EP.getExtensions()
    val metricEvents = mutableSetOf<MetricEvent>()
    for (settingsProvider in settingsProviders) {
      val languages = settingsProvider.getSupportedLanguages(project)
      for (language in languages) {
        val models = settingsProvider.createModels(project, language)
        for (model in models) {
          metricEvents.add(getModelEvent(model, language))
        }
      }
    }
    return metricEvents
  }

  private fun getModelEvent(model: InlayProviderSettingsModel, language: Language): MetricEvent {
    val usageData = FeatureUsageData()
      .addLanguage(language)
      .addData("enabled", model.isEnabled)
      .addPluginInfo(getPluginInfo(model.javaClass))
    for (case in model.cases) {
      usageData.addData(StatisticsEventEscaper.escapeFieldName(case.id), case.value)
    }
    return MetricEvent(model.id, usageData)
  }

  override fun getVersion(): Int {
    return 2
  }
}