// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newBooleanMetric
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project

class CompilerSettingsCollector : ProjectUsagesCollector() {
  override fun getGroupId() = "java.compiler.settings.project"

  override fun getMetrics(project: Project): MutableSet<MetricEvent> {
    val usages = mutableSetOf<MetricEvent>()

    val config = CompilerWorkspaceConfiguration.getInstance(project)

    usages.add(newBooleanMetric("AUTO_SHOW_ERRORS_IN_EDITOR", config.AUTO_SHOW_ERRORS_IN_EDITOR))
    usages.add(newBooleanMetric("DISPLAY_NOTIFICATION_POPUP", config.DISPLAY_NOTIFICATION_POPUP))
    usages.add(newBooleanMetric("CLEAR_OUTPUT_DIRECTORY", config.CLEAR_OUTPUT_DIRECTORY))
    usages.add(newBooleanMetric("MAKE_PROJECT_ON_SAVE", config.MAKE_PROJECT_ON_SAVE))
    usages.add(newBooleanMetric("PARALLEL_COMPILATION", config.PARALLEL_COMPILATION))
    usages.add(newBooleanMetric("REBUILD_ON_DEPENDENCY_CHANGE", config.REBUILD_ON_DEPENDENCY_CHANGE))
    usages.add(newBooleanMetric("COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT", config.COMPILE_AFFECTED_UNLOADED_MODULES_BEFORE_COMMIT))

    return usages
  }
}
