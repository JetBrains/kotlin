// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.statistics

import com.intellij.internal.statistic.collectors.fus.actions.persistence.ActionsCollectorImpl
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

class GradleActionsUsagesCollector {
  companion object {
    @JvmStatic
    fun trigger(project: Project?, action: AnAction, event: AnActionEvent?) {
      ActionsCollectorImpl.record("build.gradle.actions", project, action, event, null)
    }

    @JvmStatic
    fun trigger(project: Project?, feature: String) {
      if (project == null) return
      FUCounterUsageLogger.getInstance().logEvent(project, "build.gradle.actions", feature)
    }
  }
}
