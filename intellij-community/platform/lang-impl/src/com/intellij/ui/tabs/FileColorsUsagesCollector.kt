// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tabs

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newBooleanMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import com.intellij.ui.FileColorManager

class FileColorsUsagesCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "appearance.file.colors"

  override fun getMetrics(project: Project): MutableSet<MetricEvent> {
    val set = mutableSetOf<MetricEvent>()
    val manager = FileColorManager.getInstance(project) ?: return set
    val enabledFileColors = manager.isEnabled
    val useInEditorTabs = enabledFileColors && manager.isEnabledForTabs
    val useInProjectView = enabledFileColors && manager.isEnabledForProjectView
    if (!enabledFileColors) set.add(newBooleanMetric("file.colors", false))
    if (!useInEditorTabs) set.add(newBooleanMetric("editor.tabs", false))
    if (!useInProjectView) set.add(newBooleanMetric("project.view", false))
    return set
  }
}
