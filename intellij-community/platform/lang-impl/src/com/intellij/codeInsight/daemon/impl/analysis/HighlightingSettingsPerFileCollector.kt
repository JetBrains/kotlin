// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.analysis

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newCounterMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project

class HighlightingSettingsPerFileCollector : ProjectUsagesCollector() {
  override fun getGroupId(): String = "highlighting.settings.per.file"

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val settings = HighlightingSettingsPerFile.getInstance(project)
    return setOf(
      newCounterMetric("skip.highlighting.roots", settings.countRoots(FileHighlightingSetting.SKIP_HIGHLIGHTING)),
      newCounterMetric("skip.inspection.roots", settings.countRoots(FileHighlightingSetting.SKIP_INSPECTION))
    )
  }
}
