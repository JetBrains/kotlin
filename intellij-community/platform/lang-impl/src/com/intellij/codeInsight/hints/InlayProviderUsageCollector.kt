// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector

class InlayProviderUsageCollector : ApplicationUsagesCollector() {
  override fun getGroupId(): String {
    return "inlay.configuration"
  }

  override fun getMetrics(): MutableSet<MetricEvent> {
    val settings = InlayHintsSettings.instance()
    val providers = InlayHintsProviderExtension.findProviders()
    return providers
      .filter { it.provider.key.id in knownProviderKeys }
      .map { info ->
        val language = info.language
        val enabled = settings.hintsEnabled(info.provider.key, language)
        MetricEvent("settings", FeatureUsageData()
          .addLanguage(language)
          .addData("enabled", enabled)
        )
      }.toMutableSet()
  }

  companion object {
    private val knownProviderKeys = listOf(
      "JavaLens",
      "js.type.hints",
      "js.chain.hints",
      "ts.enum.hints",
      "annotation.hints",
      "chain.hints",
      "groovy.parameters.hints"
    )
  }
}