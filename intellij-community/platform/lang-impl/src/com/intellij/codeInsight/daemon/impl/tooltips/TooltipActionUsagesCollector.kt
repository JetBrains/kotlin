// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl.tooltips

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newBooleanMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import java.util.*

class TooltipActionUsagesCollector : ProjectUsagesCollector() {
  override fun getGroupId(): String = "editor.settings.ide"

  override fun getMetrics(project: Project): Set<MetricEvent> {
    return setOf(newBooleanMetric("show.actions.in.tooltip", TooltipActionProvider.isShowActions()))
  }
} 
